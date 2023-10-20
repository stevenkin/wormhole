package com.github.wormhole.client;

import io.netty.channel.ChannelPipeline;

public class SignalClient extends Client{

    public SignalClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        
    }
    
}
