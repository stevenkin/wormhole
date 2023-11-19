package com.github.wormhole.server.processor;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.Processor;
import com.github.wormhole.client.ack.AckHandler;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.server.ProxyServer;
import com.github.wormhole.server.Server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataTransAckProcessor implements Processor{
    private Server server;

    public DataTransAckProcessor(Server server) {
        this.server = server;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x3;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        log.info("服务器ack{}", msg);
        String proxyId = msg.getProxyId();
        ByteBuf payload = msg.getPayload();
        String serviceKey = msg.getServiceKey();
        String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
        JSONObject parseObject = JSONObject.parseObject(string);
        String string2 = parseObject.getString("channelId");
        Long long1 = parseObject.getLong("ackSize");
        ProxyServer proxyServer = server.getProxyServer(proxyId);
        if (proxyServer != null) {
            Channel channel = proxyServer.getClientHandler().getClientChannelMap().get(string2);
            if (channel != null) {
                AckHandler ackHandler = proxyServer.getAckHandlerMap().get(channel);
                if (ackHandler != null) {
                    ackHandler.setAck(long1);
                }
            }
        }
    }
    
}
