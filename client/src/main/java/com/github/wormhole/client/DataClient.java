package com.github.wormhole.client;

import java.nio.charset.Charset;

import io.netty.channel.Channel;
import org.apache.commons.lang3.RandomStringUtils;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.GenericFutureListener;

public class DataClient extends Client<ByteBuf>{
    private volatile Channel channel;

    public DataClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new DataTransHandler(this));
    }

    @Override
    protected ChannelFuture send(ByteBuf msg) {
        return channel.writeAndFlush(msg);
    }

    public void refresh(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }
}
