package com.github.wormhole.client;

import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

public class DataTransHandler extends ChannelDuplexHandler {
    private DataClient dataClient;

    public DataTransHandler(DataClient dataClient) {
        this.dataClient = dataClient;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            dataClient.getChannel().writeAndFlush(msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
    
}
