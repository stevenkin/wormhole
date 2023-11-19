package com.github.wormhole.server;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class DataTransHandler extends ChannelInboundHandlerAdapter{
    private Map<String, Channel> channalMap = new ConcurrentHashMap<>();

    private Map<Channel, Channel> clientChannelMap = new ConcurrentHashMap<>();

    private Server server;

    public DataTransHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        Channel channel = ctx.channel();
        String key = channel.remoteAddress().toString() + "-" + channel.localAddress().toString();
        channalMap.put(key, channel);
        log.info("内网代理与服务器建立数据传输通道{}", key);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        Channel channel = ctx.channel();
        channalMap.remove(channel.id().toString());
        log.info("内网代理与服务器关闭数据传输通道{}", channel);
    }

    public Channel getDataTransChannel(String channelId) {
        return channalMap.get(channelId);
    }

    public void buildDataClientChannelMap(Channel dataChannel, Channel clientChannel) {
        clientChannelMap.put(dataChannel, clientChannel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        Channel channel2 = clientChannelMap.get(channel);
        channel2.writeAndFlush(msg);
        log.info("读取来自内网代理的数据，并发送给客户端{},{}", channel, channel2);
    }

    public void clear(String clientAddress) {
        
    }
}
