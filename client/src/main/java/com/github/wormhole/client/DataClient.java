package com.github.wormhole.client;

import io.netty.channel.Channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.buffer.ByteBuf;

public class DataClient extends Client<ByteBuf>{
    private volatile Channel serviceChannel;

    public DataClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new DataTransHandler(this));
    }

    @Override
    protected ChannelFuture send(ByteBuf msg) {
        return serviceChannel.writeAndFlush(msg);
    }

    public void refresh(Channel channel) {
        this.serviceChannel = channel;
    }

    public Channel getServiceChannel() {
        return serviceChannel;
    }
}
