package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.Holder;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.ConcurrentSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.RandomStringUtils;

@Slf4j
@Sharable
public class ProxyHandler extends SimpleChannelInboundHandler<Frame> {
    private ProxyServiceConfig config;

    private ConcurrentMap<String, Channel> map = new ConcurrentHashMap<>();

    private Map<String, DataClient> dataChannelMap = new ConcurrentHashMap<>();

    private ProxyClient proxyClient;

    private DataClientPool dataClientPool;

    public ProxyHandler(ProxyClient proxyClient, ProxyServiceConfig config) {
        this.config = config;
        this.proxyClient = proxyClient;
        if (config != null) {
            this.dataClientPool = new DataClientPool(config.getServerHost(), config.getServerPort());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        proxyClient.updateHeatbeatTime();
        int opCode = msg.getOpCode();
        String serviceKey = msg.getServiceKey();
        ByteBuf payload = msg.getPayload();
        proxyClient.updateHeatbeatTime();
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
        String address = msg.getRealClientAddress();
        if (opCode == 0x9) {
            log.info("0x9 {}", msg);
            ProxyServiceConfig.ServiceConfig serviceConfig = config.getServiceConfig(serviceKey);
            ProxyClient proxyClient = new ProxyClient(null);
            proxyClient.setChannel1(ctx.channel());
            proxyClient.setServiceKey(serviceKey);
            proxyClient.setRealAddress(address);
            try {
                Channel channel = proxyClient.connect(serviceConfig.getIp(), serviceConfig.getPort());
                map.put(address, channel);
                
                DataClient client = dataClientPool.getClient();
                dataChannelMap.put(address, client);
                proxyClient.setDataClient(client);
                client.setProxyClient(proxyClient);

                Frame frame = new Frame(0xD, serviceKey, address, null);
                String key = System.currentTimeMillis() + RandomStringUtils.randomAlphabetic(8);
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                buffer.writeCharSequence(key, Charset.forName("UTF-8"));
                frame.setPayload(buffer);
                ChannelPromise send = client.send(frame);
                send.addListener(f -> {
                    Frame frame1 = new Frame(0x91, serviceKey, address, null);
                    Holder<GenericFutureListener> holder = new Holder<>();
                    GenericFutureListener listener = f1 -> {
                        if (!f1.isSuccess()) {
                            ctx.writeAndFlush(frame1).addListener(holder.t);
                        }
                    };
                    holder.t = listener;
                    ctx.writeAndFlush(frame1).addListener(holder.t);
                });
            } catch (Exception e) {
                Frame frame = new Frame(0x90, serviceKey,  localAddress.toString(), null);
                    Holder<GenericFutureListener> holder = new Holder<>();
                    GenericFutureListener listener = f1 -> {
                        if (!f1.isSuccess()) {
                            ctx.writeAndFlush(frame).addListener(holder.t);
                        }
                    };
                    holder.t = listener;
                    ctx.writeAndFlush(frame).addListener(holder.t);
            }
        } else if (opCode == 0x3) {
            log.info("收到服务器转发的请求{}", System.currentTimeMillis());
            Channel channel = map.get(address);
            if (channel == null) {
                    Frame frame = new Frame(0x40, serviceKey, localAddress.toString(), null);
                    Holder<GenericFutureListener> holder = new Holder<>();
                    GenericFutureListener listener = f1 -> {
                        if (!f1.isSuccess()) {
                            ctx.writeAndFlush(frame).addListener(holder.t);
                        }
                    };
                    holder.t = listener;
                    ctx.writeAndFlush(frame).addListener(holder.t);
            } else {
                log.info("proxy send to service data {}", payload);
                channel.writeAndFlush(payload);
                Frame frame = new Frame(0x41, serviceKey, localAddress.toString(), null);
                Holder<GenericFutureListener> holder = new Holder<>();
                    GenericFutureListener listener = f1 -> {
                        if (!f1.isSuccess()) {
                            ctx.writeAndFlush(frame).addListener(holder.t);
                        }
                    };
                    holder.t = listener;
                    ctx.writeAndFlush(frame).addListener(holder.t);
                log.info("请求发给内网服务{}", System.currentTimeMillis());
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
            log.info("server offline123");
            Channel channel = map.get(address);
            if (channel != null && channel.isActive()) {
                channel.close();
            }
            DataClient dataClient = dataChannelMap.get(address);
            if (dataClient != null) {
                dataClient.revert();
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
