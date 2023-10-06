package com.github.wandererex.wormhole.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.ConfigLoader;
import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.Holder;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;


@Slf4j
@Sharable
public class ProxyServerHandler extends SimpleChannelInboundHandler<Frame> {
    private Map<String, ProxyServer> proxyServerMap = new HashMap<>();

    private Server server;

    public ProxyServerHandler(Server server) {
        this.server = server;
    }

    public ProxyServer getProxyServer(String serviceKey) {
        return proxyServerMap.get(serviceKey);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        System.out.println("read: " + msg);
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        if (msg.getOpCode() == 0x1) {
            String s = msg.getPayload().toString(StandardCharsets.UTF_8);
            ProxyServiceConfig proxyServiceConfig = ConfigLoader.parse(s);
            buildForwardServer(proxyServiceConfig, ctx.channel());
            Frame frame = new Frame();
            frame.setOpCode(0x11);
            frame.setRealClientAddress(localAddress.toString());
            frame.setServiceKey(msg.getServiceKey());
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    ctx.writeAndFlush(frame).addListener(holder.t);
                }
            };
            holder.t = listener;
            ctx.writeAndFlush(frame).addListener(holder.t);
            System.out.println("write: " + msg);
        }
        if (msg.getOpCode() == 0xE) {
            proxyServerMap.values().forEach(p -> p.setProxyChannel(ctx.channel()));
            System.out.println("write: " + msg);
        }
        if (msg.getOpCode() == 0x5) {
            Frame frame = new Frame(0x6, null, localAddress.toString(), null);
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    ctx.writeAndFlush(frame).addListener(holder.t);
                }
            };
            holder.t = listener;
            ctx.writeAndFlush(frame).addListener(holder.t);
            System.out.println("write: " + frame);
        }
        if (msg.getOpCode() == 0x91) {
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.pass(msg.getRealClientAddress());
            }
        }
        if (msg.getOpCode() == 0x90) {
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.refuse(msg.getRealClientAddress());
            }
        }
        if (msg.getOpCode() == 0x3) {
            log.info("服务器收到响应{}", System.currentTimeMillis());
            log.info("server read from proxy data {}", msg);
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.send(msg);
            }
            Frame frame = new Frame(0x41, null, localAddress.toString(), null);
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    ctx.writeAndFlush(frame).addListener(holder.t);
                }
            };
            holder.t = listener;
            ctx.writeAndFlush(frame).addListener(holder.t);
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
        if (msg.getOpCode() == 0xD) {
            log.info("oxD {}", msg);
            ctx.fireChannelRead(msg);
        }
        if (msg.getOpCode() == 0xC) {
            log.info("oxC {}", msg);
            ProxyServer proxyServer = proxyServerMap.get(msg.getServiceKey());
            if (proxyServer != null) {
                proxyServer.getForwardHandler().cleanDataChannel(msg.getRealClientAddress());
                Frame frame = new Frame(0xC1, msg.getServiceKey(), msg.getRealClientAddress(), msg.getPayload());
                write(frame, ctx.channel());
            }
        }
    }

    private void buildForwardServer(ProxyServiceConfig config, Channel channel) {
        Map<String, ProxyServiceConfig.ServiceConfig> serviceConfigMap = config.getServiceConfigMap();
        for (Map.Entry<String, ProxyServiceConfig.ServiceConfig> config1 : serviceConfigMap.entrySet()) {
            ProxyServer proxyServer = new ProxyServer(config1.getKey(), config1.getValue().getMappingPort(), channel, server);
            proxyServerMap.put(config1.getKey(), proxyServer);
            proxyServer.open();
            log.info("port mapping open {} {} {}", config1.getKey(), config1.getValue().getPort(), config1.getValue().getMappingPort());
        }
    }

    private void write(Frame frame, Channel channel) {
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    channel.writeAndFlush(frame).addListener(holder.t);
                }
            };
            holder.t = listener;
            channel.writeAndFlush(frame).addListener(holder.t);
    }
}
