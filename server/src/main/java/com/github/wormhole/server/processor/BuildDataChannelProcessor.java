package com.github.wormhole.server.processor;

import java.nio.charset.Charset;

import com.github.wormhole.client.SignalProcessor;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.server.ProxyServer;
import com.github.wormhole.server.Server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildDataChannelProcessor implements SignalProcessor{
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
        int opCode = msg.getOpCode();
        String realClientAddress = msg.getRealClientAddress();
        String requestId = msg.getRequestId();
        String proxyId = msg.getProxyId();
        ByteBuf payload = msg.getPayload();
        ProxyServer proxyServer = server.getProxyServer(proxyId);
        if (opCode == 0x20) {
            String dataChannelId = payload.getCharSequence(0, payload.readableBytes(), Charset.forName("UTF-8")).toString();
            if (proxyServer != null) {
                Channel clientChannel = proxyServer.getClientHandler().getClientChannelMap().get(realClientAddress);
                Channel dataTransChannel = server.getDataTransServer().getDataTransHandler().getDataTransChannel(dataChannelId);
                if (clientChannel != null && dataTransChannel != null) {
                    server.getDataTransServer().getDataTransHandler().buildDataClientChannelMap(dataTransChannel, clientChannel);
                    proxyServer.getClientHandler().getDataChannelMap().put(clientChannel, dataTransChannel);
                    proxyServer.getClientHandler().success(requestId);
                } else {
                    proxyServer.getClientHandler().fail(requestId, realClientAddress);
                }
            }
        } else {
            if (proxyServer != null) {
                proxyServer.refuse(realClientAddress);
            }
        }
    }
    
}
