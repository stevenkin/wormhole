package com.github.wormhole.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
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
        channalMap.put(channel.id().toString(), channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        Channel channel = ctx.channel();
        channalMap.remove(channel.id().toString());
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
    }
}
