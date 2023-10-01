package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.NetworkUtil;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.Buffer;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@ChannelHandler.Sharable
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {
    private String serviceKey;
    private Channel proxyChannel;

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

     private Map<String, Channel> dataChannelMap = new ConcurrentHashMap<>();

     private Map<Channel, Channel> cMap = new ConcurrentHashMap<>();

     private Map<String, CountDownLatch> lMap = new ConcurrentHashMap<>();


    public ForwardHandler(String serviceKey, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.proxyChannel = proxyChannel;
    }

    public void setChannel(String client, Channel channel) {
        channelMap.put(client, channel);
    }

    public void refuse(String client) {
        Channel channel = channelMap.remove(client);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public void buildDataChannel(String address, Channel channel) {
        dataChannelMap.put(address, channel);
        if (channelMap.containsKey(address)) {
            Channel channel2 = channelMap.get(address);
            cMap.put(channel, channel2);
        }
    }

    public void cleanDataChannel(String client) {
        channelMap.remove(client);
        Channel remove = dataChannelMap.remove(client);
        if (remove != null) {
            cMap.remove(remove);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到请求{}", System.currentTimeMillis());
        String address = ((InetSocketAddress)(ctx.channel().remoteAddress())).toString();
        CountDownLatch countDownLatch = lMap.get(address);
        if (countDownLatch == null) {
            ctx.close();
            return;
        }
        countDownLatch.await();
        Channel channel = dataChannelMap.get(address);
        if (channel != null) {
            channel.writeAndFlush(msg);
        }
    }

    public void send(Frame msg) {
        Channel channel = channelMap.get(msg.getRealClientAddress());
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(msg.getPayload());
            log.info("send to client {} {}", msg.getRealClientAddress(), msg.getPayload());
        }
        log.info("响应发给客户端{}", System.currentTimeMillis());
    }

    public void closeChannel(Frame msg) {
        Channel channel = channelMap.get(msg.getRealClientAddress());
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public static org.slf4j.Logger getLog() {
        return log;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Channel getProxyChannel() {
        return proxyChannel;
    }

    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    public void send(Channel channel, ByteBuf msg) {
        Channel channel2 = cMap.get(channel);
        if (channel2 != null && channel2.isActive()) {
            channel2.writeAndFlush(msg);
        }
    }

    public void pass(String realClientAddress) {
        CountDownLatch countDownLatch = lMap.get(realClientAddress);
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public void buildLatch(String client) {
        lMap.put(client, new CountDownLatch(1));
    }

    public void removeLatch(String address) {
        lMap.remove(address);
    }
}
