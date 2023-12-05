package com.github.wormhole.server;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.ack.AckHandler;
import com.github.wormhole.common.utils.Connection;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.common.utils.RetryUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ProxyServer proxyServer;

    private Map<String, ChannelPromise> resMap = new ConcurrentHashMap<>();

    private Map<String, DataClient> dataClientMap = new ConcurrentHashMap<>();

    private Map<String, Channel> clientChannelMap = new ConcurrentHashMap<>();

    private Map<Channel, Channel> dataChannelMap = new ConcurrentHashMap<>();

    public ClientHandler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Frame frame = new Frame();
        frame.setOpCode(0x2);
        frame.setRealClientAddress(ctx.channel().remoteAddress().toString());
        frame.setRequestId(IDUtil.genRequestId());
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress(); 
        int port = localAddress.getPort();
        frame.setServiceKey(proxyServer.getServiceKey(port));
        resMap.put(frame.getRealClientAddress(), ctx.channel().newPromise());
        clientChannelMap.put(frame.getRealClientAddress(), ctx.channel());
        proxyServer.sendToProxy(frame);
        log.info("客户端连接{}", frame);
        ctx.fireChannelActive();
    }

     @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("收到客户端数据{}", ctx.channel().remoteAddress().toString());
        String string = ctx.channel().remoteAddress().toString();
        ChannelFuture channelFuture = resMap.get(string);
        if (channelFuture != null) {
            channelFuture.addListener(f -> {
                if (f.isSuccess()) {
                    Channel channel = dataChannelMap.get(ctx.channel());
                    if (channel != null && channel.isActive()) {
                        log.info("发送给代理{}", channel);
                        channel.writeAndFlush(msg).addListener(f1 -> {
                            resMap.remove(string);
                        });
                    }
                }
            });
        } else {
            Channel channel = dataChannelMap.get(ctx.channel());
            if (channel != null && channel.isActive()) {
                log.info("发送给代理{}", channel);
                channel.writeAndFlush(msg);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (!(obj instanceof ChannelInputShutdownEvent)) {
            return;
        }
        log.info("ChannelInputShutdownEvent{}", ctx.channel().remoteAddress().toString());

        String address = ctx.channel().remoteAddress().toString();
        clear(address);
        
        proxyServer.getServer().getDataTransServer().getDataTransHandler().clear(address);

        Frame frame = new Frame();
        frame.setOpCode(0x4);
        frame.setRealClientAddress(address);
        frame.setRequestId(IDUtil.genRequestId());
        InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress(); 
        int port = localAddress.getPort();
        frame.setServiceKey(proxyServer.getServiceKey(port));
        
        AckHandler ackHandler = proxyServer.getAckHandlerMap().get(ctx.channel());
        if (ackHandler != null) {
            if (ackHandler.isAckComplate()) {
                log.info("数据已全部发给内网服务{}", frame);
                proxyServer.sendToProxy(frame);
                return;
           }
           ChannelPromise newPromise = ctx.channel().newPromise();
           ackHandler.setPromise(newPromise);
           newPromise.addListener(f -> {
               log.info("数据已全部发给内网服务{}", frame);
                proxyServer.sendToProxy(frame);
           });   
        }
    }

    public void success(String id) {
        ChannelPromise remove = resMap.get(id);
        if (remove != null) {
            remove.setSuccess();
        }
    }

    public void fail(String address) {
        ChannelPromise remove = resMap.get(address);
        if (remove != null) {
            remove.setFailure(new RuntimeException());
        }
        refuse(address);
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Map<String, ChannelPromise> getResMap() {
        return resMap;
    }

    public Map<String, DataClient> getDataClientMap() {
        return dataClientMap;
    }

    public Map<String, Channel> getClientChannelMap() {
        return clientChannelMap;
    }

    public void refuse(String realClientAddress) {
        Channel channel = clientChannelMap.remove(realClientAddress);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public Map<Channel, Channel> getDataChannelMap() {
        return dataChannelMap;
    }

    public void clear(String clientAddress) {

    }
    
}
