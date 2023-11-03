package com.github.wormhole.proxy.processor;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.Processor;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class DataTransAckProcessor implements Processor{
    private Proxy proxy;

    public DataTransAckProcessor(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x3;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        String proxyId = msg.getProxyId();
        ByteBuf payload = msg.getPayload();
        String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
        JSONObject parseObject = JSONObject.parseObject(string);
        String string2 = parseObject.getString("channelId");
        Long long1 = parseObject.getLong("ackSize");
        DataClient assignedDataClient = proxy.getDataClientPool().getAssignedDataClient(string2);
        if (assignedDataClient != null) {
            assignedDataClient.setAck(long1);
        }
    }
    
}