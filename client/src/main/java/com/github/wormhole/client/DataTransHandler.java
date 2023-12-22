package com.github.wormhole.client;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.wormhole.client.ack.AckHandler;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataTransHandler extends ChannelInboundHandlerAdapter {
    private DataClient dataClient;

    public DataTransHandler(DataClient dataClient) {
        this.dataClient = dataClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("转发对端传来的数据{}-{}", dataClient, dataClient.getDirectClient());
        dataClient.getDirectClient().send((ByteBuf) msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (dataClient.getConnType() != 2) {
            return;
        }
        Frame closePeer = closePeer();
        AckHandler ackHandler = dataClient.getAckHandler();

       if (ackHandler.isAckComplate()) {
           log.info("内网服务关闭连接{}", closePeer);
            dataClient.getContext().write(closePeer);
            return;
       }
       ChannelPromise newPromise = ctx.channel().newPromise();
       ackHandler.setPromise(newPromise);
       newPromise.addListener(f -> {
            log.info("内网服务关闭连接{}", closePeer);
            dataClient.getContext().write(closePeer);
       });
    }

    private Frame closePeer() {
        Frame frame = new Frame();
        frame.setOpCode(0x4);
        frame.setProxyId(dataClient.getContext().id());
        frame.setServiceKey(dataClient.getDataClientPool().getServiceKey());
        frame.setRealClientAddress(dataClient.getPeerClientAddress());
        frame.setRequestId(IDUtil.genRequestId());
        return frame;
    }
}
