package com.github.wandererex.wormhole.server;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.Getter;

@Sharable
public class DataForwardHander extends ChannelInboundHandlerAdapter {
    private Map<Channel, String> channelMap = new ConcurrentHashMap<>();

    @Getter
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (channelMap.containsKey(ctx.channel())) {
            String string = channelMap.get(ctx.channel());
            ProxyServer proxyServer = proxyServerHandler.getProxyServer(string);
            if (proxyServer != null) {
                proxyServer.forwardData(ctx.channel(), (ByteBuf) msg);
            }
        }
    }
    
}
