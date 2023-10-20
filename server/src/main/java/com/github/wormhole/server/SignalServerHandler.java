package com.github.wormhole.server;

import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SignalServerHandler extends SimpleChannelInboundHandler<Frame>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        
    }

}
