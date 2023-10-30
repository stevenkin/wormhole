package com.github.wormhole.client;

import java.util.ArrayList;
import java.util.List;

import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class SignalHandler extends SimpleChannelInboundHandler<Frame>{
    private List<SignalProcessor> list = new ArrayList<>();

    public SignalHandler register(SignalProcessor signalProcessor) {
        this.list.add(signalProcessor);
        return this;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        for (SignalProcessor processor : list) {
            if (processor.isSupport(msg)) {
                processor.process(ctx, msg);
            }
        }
    }
    
}
