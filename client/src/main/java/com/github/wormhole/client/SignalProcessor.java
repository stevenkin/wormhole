package com.github.wormhole.client;

import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelHandlerContext;

public interface SignalProcessor {
    boolean isSupport(Frame frame);

    void process(ChannelHandlerContext ctx, Frame msg);
}
