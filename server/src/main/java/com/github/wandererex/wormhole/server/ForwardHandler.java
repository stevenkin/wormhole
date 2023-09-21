package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.NetworkUtil;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
@Slf4j
public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String serviceKey;
    private Channel proxyChannel;

    private Map<String, Channel> channelMap = new HashMap<>();


    public ForwardHandler(String serviceKey, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.proxyChannel = proxyChannel;
    }

    public void setChannel(String client, Channel channel) {
        channelMap.put(client, channel);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        log.info("收到请求{}", System.currentTimeMillis());
        String address = ((InetSocketAddress)(ctx.channel().remoteAddress())).toString();
        AttributeKey<CountDownLatch> attributeKey = AttributeKey.valueOf(serviceKey);
        Attribute<CountDownLatch> attr = proxyChannel.attr(attributeKey);
        if (attr.get() != null) {
            CountDownLatch latch = attr.get();
            if (latch != null) {
                latch.await();
            }
            attr.set(null);
        }
        ByteBuf copy = null;
        try {
            copy = msg.copy();
            List<Frame> frames = NetworkUtil.byteArraytoFrameList(copy, serviceKey, address);
            List<ChannelFuture> channelFutures = new ArrayList<>();
            for (Frame frame : frames) {
                log.info("server mapping port read {}", frame);
                ChannelFuture writeAndFlush = proxyChannel.writeAndFlush(frame);
                channelFutures.add(writeAndFlush);
            }
            
            for (ChannelFuture future : channelFutures) {
                future.sync();
            }
            log.info("收到请求,转发给代理{}", System.currentTimeMillis());
        } catch (Exception e) {

        } finally {
            if (copy != null) {
                ReferenceCountUtil.release(copy);
            }
        }
    }

    public void send(Frame msg) {
        Channel channel = channelMap.get(msg.getRealClientAddress());
        if (channel != null && channel.isActive()) {
            try {
                channel.writeAndFlush(msg.getPayload()).sync();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            log.info("send to client {} {}", msg.getRealClientAddress(), msg.getPayload());
        }
        log.info("响应发给客户端{}", System.currentTimeMillis());
    }

    public void closeChannel(Frame msg) {
        Channel channel = channelMap.get(msg.getRealClientAddress());
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }
}
