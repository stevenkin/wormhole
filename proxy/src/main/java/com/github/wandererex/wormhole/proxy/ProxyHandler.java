package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ProxyHandler extends SimpleChannelInboundHandler<Frame> {
    private ProxyServiceConfig config;

    private ConcurrentMap<String, Channel> map = new ConcurrentHashMap<>();

    private ProxyClient proxyClient;

    public ProxyHandler(ProxyClient proxyClient, ProxyServiceConfig config) {
        this.config = config;
        this.proxyClient = proxyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        int opCode = msg.getOpCode();
        String serviceKey = msg.getServiceKey();
        byte[] payload = msg.getPayload();
        if (opCode == 0x9) {
            ProxyServiceConfig.ServiceConfig serviceConfig = config.getServiceConfig(serviceKey);
            ProxyClient proxyClient = new ProxyClient(null);
            try {
                Channel channel = proxyClient.connect(serviceConfig.getIp(), serviceConfig.getPort());
                map.put(serviceKey, channel);
                Frame frame = new Frame(0x91, serviceKey, null);
                ctx.write(frame);
            } catch (Exception e) {
                Frame frame = new Frame(0x90, serviceKey, null);
                ctx.write(frame);
            }
        } else if (opCode == 0x3) {
            Channel channel = map.get(serviceKey);
            if (channel == null) {
                Frame frame = new Frame(0x40, serviceKey, null);
                ctx.write(frame);
            } else {
                channel.writeAndFlush(payload);
                Frame frame = new Frame(0x41, serviceKey, null);
                ctx.write(frame);
            }
        } else if (opCode == 0x10) {
            log.error("proxy connect server error");
            close();
        } else if (opCode == 0x11) {
            log.info("proxy connect server success");
            proxyClient.authSuccess();
        } else if (opCode == 0x6) {
            log.info("proxy update heatbeat time");
            proxyClient.updateHeatbeatTime();
        } else if (opCode == 0x81) {
            log.info("proxy offline success");
            close();
        } else if (opCode == 0x80) {
            log.error("proxy offline error");
        } else if (opCode == 0x7) {
            log.info("server offline");
            Frame frame = new Frame(0x81, null, null);
            proxyClient.send(frame);
            close();
        }
    }

    private void close() throws Exception {
        proxyClient.shutdown();
        for (Channel channel : map.values()) {
            channel.close();
        }
    }
}
