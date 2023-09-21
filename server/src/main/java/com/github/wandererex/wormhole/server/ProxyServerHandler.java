package com.github.wandererex.wormhole.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ProxyServerHandler extends SimpleChannelInboundHandler<Frame> {
    private Map<String, ProxyServer> proxyServerMap = new HashMap<>();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        System.out.println("read: " + msg);
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        if (msg.getOpCode() == 0x1) {
            String s = msg.getPayload().toString(StandardCharsets.UTF_8);
            JSONObject jsonObject = JSON.parseObject(s);
            ProxyServiceConfig proxyServiceConfig = new ProxyServiceConfig();
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                proxyServiceConfig.addConfig(entry.getKey(), JSON.parseObject((String) entry.getValue()).toJavaObject(ProxyServiceConfig.ServiceConfig.class));
            }
            buildForwardServer(proxyServiceConfig, ctx.channel());
            Frame frame = new Frame();
            frame.setOpCode(0x11);
            frame.setRealClientAddress(localAddress.toString());
            frame.setServiceKey(msg.getServiceKey());
            ctx.writeAndFlush(frame);
            System.out.println("write: " + msg);
        }
        if (msg.getOpCode() == 0x5) {
            Frame frame = new Frame(0x6, null, localAddress.toString(), null);
            ctx.writeAndFlush(frame);
            System.out.println("write: " + frame);
        }
        if (msg.getOpCode() == 0x91) {
            AttributeKey<CountDownLatch> attributeKey = AttributeKey.valueOf(msg.getServiceKey());
            Attribute<CountDownLatch> attr = ctx.attr(attributeKey);
            CountDownLatch latch = attr.get();
            if (latch != null) {
                latch.countDown();
            }
        }
        if (msg.getOpCode() == 0x3) {
            log.info("server read from proxy data {}", msg);
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.send(msg);
            }
            Frame frame = new Frame(0x41, null, localAddress.toString(), null);
            ctx.writeAndFlush(frame);
            System.out.println("write: " + frame);
        }
        if (msg.getOpCode() == 0x40) {
            System.out.println("write data error");
        }
        if (msg.getOpCode() == 0x41) {
            System.out.println("write data success");
        }
        if (msg.getOpCode() == 0xB) {
            log.info("server offline");
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.closeChannel(msg);
            }
        }
    }

    private void buildForwardServer(ProxyServiceConfig config, Channel channel) {
        Map<String, ProxyServiceConfig.ServiceConfig> serviceConfigMap = config.getServiceConfigMap();
        for (Map.Entry<String, ProxyServiceConfig.ServiceConfig> config1 : serviceConfigMap.entrySet()) {
            ProxyServer proxyServer = new ProxyServer(config1.getKey(), config1.getValue().getMappingPort(), channel);
            proxyServerMap.put(config1.getKey(), proxyServer);
            proxyServer.open();
            log.info("port mapping open {} {} {}", config1.getKey(), config1.getValue().getPort(), config1.getValue().getMappingPort());
        }
    }
}
