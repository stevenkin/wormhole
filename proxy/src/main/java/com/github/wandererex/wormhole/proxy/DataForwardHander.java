package com.github.wandererex.wormhole.proxy;

import java.net.InetSocketAddress;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DataForwardHander extends SimpleChannelInboundHandler<ByteBuf> {

    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        
    }
    
}
