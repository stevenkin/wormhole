package com.github.wormhole.client;

import io.netty.channel.Channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.buffer.ByteBuf;

public class DataClient extends Client<ByteBuf>{
    private volatile DataClient directClient;

    public DataClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    public void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new DataTransHandler(this));
    }

    @Override
    public ChannelFuture send(ByteBuf msg) {
        return channel.writeAndFlush(msg);
    }

    public void refresh(DataClient directClient) {
        this.directClient = directClient;
    }

    public DataClient getDirectChannel() {
        return directClient;
    }
}
