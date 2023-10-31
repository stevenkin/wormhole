package com.github.wormhole.server;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.wormhole.client.DataClient;
import com.github.wormhole.common.utils.Connection;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.common.utils.RetryUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
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
        resMap.put(frame.getRequestId(), ctx.channel().newPromise());
        clientChannelMap.put(frame.getRealClientAddress(), ctx.channel());
        proxyServer.sendToProxy(frame);
    }

     @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String string = ctx.channel().remoteAddress().toString();
        ChannelFuture channelFuture = resMap.get(string);
        if (channelFuture != null) {
            channelFuture.addListener(f -> {
                if (f.isSuccess()) {
                    DataClient dataClient = dataClientMap.get(string);
                    if (dataClient == null) {
                        dataClient = proxyServer.getDataClientPool().take();
                        dataClientMap.put(string, dataClient);
                    }
                    DataClient dataClient1 = dataClient;
                    Connection connection = new Connection() {
                        @Override
                        public ChannelFuture write(Object msg) {
                            return dataClient1.send((ByteBuf) msg);
                        }
                    };
                    RetryUtil.writeLimitNumThen(connection, msg, 3, () -> {
                        resMap.remove(string);
                    });
                }
            });
        }
    }

    public void success(String id) {
        ChannelPromise remove = resMap.get(id);
        if (remove != null) {
            remove.setSuccess();
        }
    }

    public void fail(String id, String address) {
        ChannelPromise remove = resMap.get(id);
        if (remove != null) {
            remove.setFailure(null);
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
    
}
