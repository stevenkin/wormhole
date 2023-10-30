package com.github.wormhole.server.processor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.github.wormhole.client.SignalProcessor;
import com.github.wormhole.common.config.ProxyServiceConfig;
import com.github.wormhole.common.utils.ConfigLoader;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.server.ProxyServer;
import com.github.wormhole.server.Server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyRegisterProcessor implements SignalProcessor{
    private Server server;
    
    public ProxyRegisterProcessor(Server server) {
        this.server = server;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x1;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        String s = msg.getPayload().toString(StandardCharsets.UTF_8);
        ProxyServiceConfig proxyServiceConfig = ConfigLoader.parse(s);
        String proxyId = server.buildProxyServer(proxyServiceConfig, ctx.channel());
    }
    
}
