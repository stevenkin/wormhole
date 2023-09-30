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
    private Map<Channel, String> channelMap = new ConcurrentHashMap<>();

    private ProxyServerHandler proxyServerHandler;

    public DataForwardHander(ProxyServerHandler proxyServerHandler) {
        this.proxyServerHandler = proxyServerHandler;
    }

    public void setChannel(String address, String serviceKey, Channel channel) {
        channelMap.put(channel, serviceKey);
        ProxyServer proxyServer = proxyServerHandler.getProxyServer(serviceKey);
        if (proxyServer != null) {
            proxyServer.buildDataChannel(address, channel);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (channelMap.containsKey(ctx.channel())) {
            String string = channelMap.get(ctx.channel());
            ProxyServer proxyServer = proxyServerHandler.getProxyServer(string);
            if (proxyServer != null) {
                proxyServer.forwardData(ctx.channel(), msg);
            }
        }
    }
    
}
