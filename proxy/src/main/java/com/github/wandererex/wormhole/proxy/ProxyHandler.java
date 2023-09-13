package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
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
        proxyClient.updateHeatbeatTime();
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        String address = msg.getRealClientAddress();
        if (opCode == 0x9) {
            ProxyServiceConfig.ServiceConfig serviceConfig = config.getServiceConfig(serviceKey);
            ProxyClient proxyClient = new ProxyClient(null);
            proxyClient.setChannel1(ctx.channel());
            proxyClient.setServiceKey(serviceKey);
            proxyClient.setReadAddress(address);
            try {
                Channel channel = proxyClient.connect(serviceConfig.getIp(), serviceConfig.getPort());
                map.put(address, channel);
                Frame frame = new Frame(0x91, serviceKey, localAddress.toString(), null);
                ctx.writeAndFlush(frame);
            } catch (Exception e) {
                Frame frame = new Frame(0x90, serviceKey,  localAddress.toString(), null);
                ctx.writeAndFlush(frame);
            }
        } else if (opCode == 0x3) {
            Channel channel = map.get(address);
            if (channel == null) {
                Frame frame = new Frame(0x40, serviceKey, localAddress.toString(), null);
                ctx.writeAndFlush(frame);
            } else {
                log.info("proxy send to service data {}", payload);
                TaskExecutor.get().addTask(new Task(channel, Unpooled.copiedBuffer(payload)));
                Frame frame = new Frame(0x41, serviceKey, localAddress.toString(), null);
                ctx.writeAndFlush(frame);
            }
        } else if (opCode == 0x10) {
            log.error("proxy connect server error");
            close();
        } else if (opCode == 0x11) {
            log.info("proxy connect server success");
            proxyClient.authSuccess();
        } else if (opCode == 0x6) {
            log.info("proxy update heatbeat time");
        } else if (opCode == 0x81) {
            log.info("proxy offline success");
            close();
        } else if (opCode == 0x80) {
            log.error("proxy offline error");
        } else if (opCode == 0x7) {
            log.info("server offline");
            Frame frame = new Frame(0x81, null, localAddress.toString(), null);
            proxyClient.send(frame);
            close();
        } else if (opCode == 0xA) {
            log.info("server offline");
            Channel channel = map.get(address);
            if (channel != null) {
                channel.close();
            }
        }
    }

    private void close() throws Exception {
        proxyClient.shutdown();
        for (Channel channel : map.values()) {
            channel.close();
        }
    }
}
