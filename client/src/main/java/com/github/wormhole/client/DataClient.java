package com.github.wormhole.client;

import io.netty.channel.Channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;

import com.github.wormhole.client.ack.AckHandler;

import io.netty.buffer.ByteBuf;

public class DataClient extends Client<ByteBuf>{
    private volatile DataClient directClient;

    private DataTransHandler dataTransHandler = new DataTransHandler(this);

    /**
     * 连接类型（1.连接到代理服务器， 2.连接到内网服务）
     */
    private int connType;

    private String peerClientAddress;

    private DataClientPool dataClientPool;

    private AckHandler ackHandler;

    private Context context;

    public DataClient(String ip, Integer port, int connType, Context context, DataClientPool dataClientPool) {
        super(ip, port, context);
        this.connType = connType;
        this.dataClientPool = dataClientPool;
        this.context = context;
    }

    @Override
    public void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(dataTransHandler);
        if (connType == 2) {
            this.ackHandler = new AckHandler(channel, context, context.id(), dataClientPool.getServiceKey(), peerClientAddress);
            pipeline.addLast(ackHandler);   
        }
    }

    @Override
    public ChannelFuture send(ByteBuf msg) {
        return channel.writeAndFlush(msg);
    }

    public void refresh(DataClient directClient) {
        this.directClient = directClient;
    }

    public void revert() {
        dataClientPool.revert(this);
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

    // public void setAck(long num) {
    //     channel.eventLoop().submit(() -> {
    //         dataTransHandler.setAck(num);
    //     });
    // }

    public String getPeerClientAddress() {
        return peerClientAddress;
    }

    public void setDirectClient(DataClient directClient) {
        this.directClient = directClient;
    }

    public void setDataTransHandler(DataTransHandler dataTransHandler) {
        this.dataTransHandler = dataTransHandler;
    }

    public void setConnType(int connType) {
        this.connType = connType;
    }

    public void setPeerClientAddress(String peerClientAddress) {
        this.peerClientAddress = peerClientAddress;
        dataClientPool.setClientAssignedPeer(peerClientAddress, getId());
        if (connType == 1) {
            return;
        }
        ackHandler.setPeerClientAddress(peerClientAddress);
    }

    public DataClientPool getDataClientPool() {
        return dataClientPool;
    }

    public void setDataClientPool(DataClientPool dataClientPool) {
        this.dataClientPool = dataClientPool;
    }

    public AckHandler getAckHandler() {
        return ackHandler;
    }

    public Context getContext() {
        return context;
    }
}
