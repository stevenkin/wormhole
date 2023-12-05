package com.github.wormhole.server.processor;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import com.github.wormhole.client.Processor;
import com.github.wormhole.client.ack.AckHandler;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.server.ProxyServer;
import com.github.wormhole.server.Server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildDataChannelProcessor implements Processor{
    private Server server;

    public BuildDataChannelProcessor(Server server) {
        this.server = server;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x20 ||  frame.getOpCode() == -0x20;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        log.info("内网代理与服务器建立数据通道成功{}", msg);
        int opCode = msg.getOpCode();
        String realClientAddress = msg.getRealClientAddress();
        String requestId = msg.getRequestId();
        String proxyId = msg.getProxyId();
        ByteBuf payload = msg.getPayload();
        ProxyServer proxyServer = server.getProxyServer(proxyId);
        String serviceKey = msg.getServiceKey();
        if (opCode == 0x20) {
            String dataChannelId = payload.getCharSequence(0, payload.readableBytes(), Charset.forName("UTF-8")).toString();
            if (proxyServer != null) {
                Channel clientChannel = proxyServer.getClientHandler().getClientChannelMap().get(realClientAddress);
                Channel dataTransChannel = server.getDataTransServer().getDataTransHandler().getDataTransChannel(dataChannelId);
                if (clientChannel != null && dataTransChannel != null) {
                    server.getDataTransServer().getDataTransHandler().buildDataClientChannelMap(dataTransChannel, clientChannel);
                    proxyServer.getClientHandler().getDataChannelMap().put(clientChannel, dataTransChannel);
                    server.getDataChannelProxyIdMap().put(dataChannelId, proxyId);
                    proxyServer.getClientHandler().success(realClientAddress);
                } else {
                    log.info("内网代理与服务器fail{},{},{}", msg, clientChannel, dataTransChannel);
                    proxyServer.getClientHandler().fail(realClientAddress);
                }
            }
        } else {
            if (proxyServer != null) {
                proxyServer.refuse(realClientAddress);
            }
        }
    }
    
}
