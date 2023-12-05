package com.github.wormhole.proxy.processor;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.Processor;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.info("代理ack{}", msg);
        String proxyId = msg.getProxyId();
        ByteBuf payload = msg.getPayload();
        String serviceKey = msg.getServiceKey();
        String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
        JSONObject parseObject = JSONObject.parseObject(string);
        String string2 = parseObject.getString("channelId");
        Long long1 = parseObject.getLong("ackSize");
        DataClientPool dataClientPool = proxy.getDataChannelProcessor().getServiceClientPool().get(serviceKey);
        if (dataClientPool != null) {
            String s = dataClientPool.getDataClientAssignedPeerMap().get(string2);
            if (s != null) {
                DataClient assignedDataClient = dataClientPool.getAssignedDataClient(s);
                if (assignedDataClient != null) {
                    assignedDataClient.getAckHandler().setAck(long1);
                }
            }
        }
    }
    
}
