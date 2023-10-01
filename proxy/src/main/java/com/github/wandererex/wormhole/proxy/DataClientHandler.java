package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

public class DataClientHandler extends ChannelInboundHandlerAdapter {
    private ProxyClient proxyClient;

    public void setProxyClient(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (proxyClient != null) {
            proxyClient.send((ByteBuf) msg);
        }
    }

}
