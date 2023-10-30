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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ProxyServer proxyServer;

    private Map<String, ChannelFuture> resMap = new ConcurrentHashMap<>();

    private Map<String, DataClient> dataClientMap = new ConcurrentHashMap<>();

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
                    RetryUtil.writeLimitNum(connection, msg, 3);
                }
            });
        }
    }
    
}
