package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.NetworkUtil;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
@Slf4j
public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String serviceKey;
    private Channel proxyChannel;

    private Channel channel;

    public ForwardHandler(String serviceKey, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.proxyChannel = proxyChannel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (channel == null || !channel.isActive()) {
            ctx.fireChannelRead(msg);
        }
        AttributeKey<CountDownLatch> attributeKey = AttributeKey.valueOf(serviceKey);
        Attribute<CountDownLatch> attr = proxyChannel.attr(attributeKey);
        if (attr.get() != null) {
            CountDownLatch latch = attr.get();
            if (latch != null) {
                latch.await();
            }
            attr.set(null);
        }
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes, 0, bytes.length);
        List<Frame> frames = NetworkUtil.byteArraytoFrameList(bytes, serviceKey);
        for (Frame frame : frames) {
            log.info("server mapping port read {}", frame);
            TaskExecutor.get().addTask(new Task(proxyChannel, frame));
        }
    }

    public void send(byte[] buf) {
        log.info("server send to client data {}, {}", buf, channel);
        channel.writeAndFlush(buf);
    }
}
