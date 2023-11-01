package com.github.wormhole.client;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

public class DataTransHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private DataClient dataClient;

    private long sendSize;

    private long ackSize;

    private BlockingQueue<ChannelPromise> queue = new ArrayBlockingQueue<>(1);

    public DataTransHandler(DataClient dataClient) {
        this.dataClient = dataClient;
        init();
    }

    public void init() {
        sendSize = 0;
        ackSize = 0;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        sendSize += msg.readableBytes();
        dataClient.getDirectClient().send(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
       if (sendSize == ackSize) {
            return;
       }
       ChannelPromise newPromise = ctx.channel().newPromise();
       queue.add(newPromise);
       newPromise.addListener(f -> {
            ctx.channel().close();
       });
    }

    public void setAck(long num) {
        this.ackSize = num;
        if (ackSize == sendSize) {
            ChannelPromise promise = queue.poll();
            if (promise != null) {
                promise.setSuccess();
            }
        }
    }
}
