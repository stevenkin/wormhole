package com.github.wormhole.client;

import io.netty.channel.ChannelPipeline;

public class DataClient extends Client{

    public DataClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        
    }
    
}
