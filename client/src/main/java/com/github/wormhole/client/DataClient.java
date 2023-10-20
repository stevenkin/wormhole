package com.github.wormhole.client;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

public class DataClient extends Client{

    public DataClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        
    }

    @Override
    protected <ByteBuf> ChannelPromise send(ByteBuf msg) {
        return null;
    }
    
}
