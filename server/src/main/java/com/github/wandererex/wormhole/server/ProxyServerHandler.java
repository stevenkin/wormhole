package com.github.wandererex.wormhole.server;

import com.alibaba.fastjson.JSON;
import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProxyServerHandler extends SimpleChannelInboundHandler<Frame> {
    private Map<String, ProxyServer> proxyServerMap = new HashMap<>();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        System.out.println("read: " + msg);
        if (msg.getOpCode() == 0x1) {
            byte[] payload = msg.getPayload();
            String s = new String(payload, StandardCharsets.UTF_8);
            ProxyServiceConfig proxyServiceConfig = JSON.parseObject(s, ProxyServiceConfig.class);
            buildForwardServer(proxyServiceConfig, ctx.channel());
            msg.setOpCode(0x11);
            ctx.writeAndFlush(msg);
            System.out.println("write: " + msg);
        }
        if (msg.getOpCode() == 0x5) {
            Frame frame = new Frame(0x6, null, null);
            ctx.writeAndFlush(frame);
            System.out.println("write: " + frame);
        }
    }

    private void buildForwardServer(ProxyServiceConfig config, Channel channel) {
        Map<String, ProxyServiceConfig.ServiceConfig> serviceConfigMap = config.getServiceConfigMap();
        for (Map.Entry<String, ProxyServiceConfig.ServiceConfig> config1 : serviceConfigMap.entrySet()) {
            ProxyServer proxyServer = new ProxyServer(config1.getKey(), config1.getValue().getMappingPort(), channel);
            proxyServerMap.put(config1.getKey(), proxyServer);
            proxyServer.open();
        }
    }
}
