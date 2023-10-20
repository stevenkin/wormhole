package com.github.wormhole.client;

import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

public class SignalClient extends Client{

    public SignalClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        
    }

    @Override
    protected <Frame>  ChannelPromise send(Frame msg) {
        return null;
    }
    
}
