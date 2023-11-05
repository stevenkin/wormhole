package com.github.wormhole.server.processor;

import com.github.wormhole.client.Context;

import io.netty.channel.Channel;

public class SignalChannelContext implements Context{
    private Channel signalChannel;

    public SignalChannelContext(Channel signalChannel) {
        this.signalChannel = signalChannel;
    }

    @Override
    public void write(Object msg) {
        signalChannel.writeAndFlush(msg);
    }

    @Override
    public Object read() {
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public String id() {
        throw new UnsupportedOperationException("Unimplemented method 'id'");
    }
    
}
