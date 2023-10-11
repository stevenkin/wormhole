package com.github.wandererex.wormhole.proxy;

import java.nio.charset.Charset;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

public class DataClientCmdHandler extends SimpleChannelInboundHandler<Frame> {
    private DataClient dataClient;

    public DataClientCmdHandler(DataClient dataClient) {
        this.dataClient = dataClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
            if (msg.getOpCode() == 0xD1 || msg.getOpCode() == 0xC21) {
                    ByteBuf payload = msg.getPayload();
                    String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
                    if (dataClient.getReqMap().containsKey(string)) {
                        ChannelPromise channelPromise = dataClient.getReqMap().get(string);
                        channelPromise.setSuccess();
                    }
            }
    }


}
