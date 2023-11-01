package com.github.wormhole.client;

import io.netty.channel.Channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.buffer.ByteBuf;

public class DataClient extends Client<ByteBuf>{
    private volatile DataClient directClient;

    private DataTransHandler dataTransHandler = new DataTransHandler(this);

    /**
     * 连接类型（1.连接到代理服务器， 2.连接到内网服务）
     */
    private int connType;

    public DataClient(String ip, Integer port, int connType) {
        super(ip, port);
        this.connType = connType;
    }

    @Override
    public void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(dataTransHandler);
    }

    @Override
    public ChannelFuture send(ByteBuf msg) {
        return channel.writeAndFlush(msg);
    }

    public void refresh(DataClient directClient) {
        this.directClient = directClient;
    }

    public DataClient getDirectClient() {
        return directClient;
    }

    public int getConnType() {
        return connType;
    }

    public DataTransHandler getDataTransHandler() {
        return dataTransHandler;
    }

    public void setAck(long num) {
        channel.eventLoop().submit(() -> {
            dataTransHandler.setAck(num);
        });
    }
    
}
