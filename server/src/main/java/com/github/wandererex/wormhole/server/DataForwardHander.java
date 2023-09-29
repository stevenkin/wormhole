package com.github.wandererex.wormhole.server;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DataForwardHander extends SimpleChannelInboundHandler<ByteBuf> {
    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    private ForwardHandler forwardHandler;

    public void setChannel(String key, Channel channel) {
        channelMap.put(key, channel);
        forwardHandler.buildDataChannel(key, channel);
    }

    public void setForwanrdHandler(ForwardHandler forwardHandler) {
        this.forwardHandler = forwardHandler;
    }

    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        
    }
    
}
