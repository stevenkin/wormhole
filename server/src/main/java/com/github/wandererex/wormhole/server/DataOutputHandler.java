package com.github.wandererex.wormhole.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DataOutputHandler extends ChannelOutboundHandlerAdapter {
    private ForwardHandler forwardHandler;

    private String address;

    public DataOutputHandler(ForwardHandler forwardHandler, String address) {
        this.forwardHandler = forwardHandler;
        this.address = address;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ChannelPromise clientPromiss = forwardHandler.getClientPromiss(address);
        if (clientPromiss != null) {
            clientPromiss.addListener(f -> {
                ctx.write(msg, promise);
            });
        }
    }
}
