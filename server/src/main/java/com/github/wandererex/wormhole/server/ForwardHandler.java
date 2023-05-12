package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String serviceKey;
    private Channel proxyChannel;

    public ForwardHandler(String serviceKey, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.proxyChannel = proxyChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Frame frame = new Frame(0x9, serviceKey, null);
        proxyChannel.writeAndFlush(frame);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Frame frame = new Frame(0x3, serviceKey, msg.array());
        proxyChannel.writeAndFlush(frame);
    }
}
