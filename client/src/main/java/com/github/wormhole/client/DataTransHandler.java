package com.github.wormhole.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DataTransHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private DataClient dataClient;

    public DataTransHandler(DataClient dataClient) {
        this.dataClient = dataClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        dataClient.getServiceChannel().writeAndFlush(msg);
    }

}
