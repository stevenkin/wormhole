package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String serviceKey;
    private Channel proxyChannel;

    private Channel channel;

    public ForwardHandler(String serviceKey, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.proxyChannel = proxyChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Frame frame = new Frame(0x9, serviceKey, null);
        CountDownLatch latch = new CountDownLatch(1);
        channel = ctx.channel();
        AttributeKey<CountDownLatch> attributeKey = AttributeKey.valueOf(serviceKey);
        Attribute<CountDownLatch> attr = proxyChannel.attr(attributeKey);
        attr.set(latch);
        proxyChannel.writeAndFlush(frame);
        if (latch != null) {
            latch.await();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        AttributeKey<CountDownLatch> attributeKey = AttributeKey.valueOf(serviceKey);
        Attribute<CountDownLatch> attr = proxyChannel.attr(attributeKey);
        if (attr.get() != null) {
            attr.set(null);
        }
        /*byte[] array = msg.array();
        int n = 0;
        byte[] bytes = new byte[1024];
        for (int i = 0; i < array.length; i += n) {
            if (i + 1024 < array.length) {
                System.arraycopy(array, i, bytes, 0, 1024);
                n = 1024;
                Frame frame = new Frame(0x3, serviceKey, bytes);
                proxyChannel.writeAndFlush(frame);
            } else {
                byte[] bytes1 = new byte[array.length - i];
                System.arraycopy(array, i, bytes, 0, bytes1.length);
                n = bytes1.length;
                Frame frame = new Frame(0x3, serviceKey, bytes);
                proxyChannel.writeAndFlush(frame);
            }
        }*/
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes, 0, bytes.length);
        Frame frame = new Frame(0x3, serviceKey, bytes);
        proxyChannel.writeAndFlush(frame);
    }

    public void send(byte[] buf) {
        channel.writeAndFlush(buf);
    }
}
