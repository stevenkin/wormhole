package com.github.wandererex.wormhole.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataOutputHandler extends ChannelOutboundHandlerAdapter {
    private ForwardHandler forwardHandler;

    private String address;

    public DataOutputHandler(ForwardHandler forwardHandler, String address) {
        this.forwardHandler = forwardHandler;
        this.address = address;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.info("DataOutputH {}", msg);
        ChannelPromise clientPromiss = forwardHandler.getClientPromiss(address);
        if (clientPromiss != null) {
            log.info("clientPromiss != null {}", msg);
            clientPromiss.addListener(f -> {
                ctx.writeAndFlush(msg, promise);
            });
        }
    }
}
