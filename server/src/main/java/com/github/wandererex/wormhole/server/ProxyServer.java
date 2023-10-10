package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.Task;
import com.github.wandererex.wormhole.serialize.TaskExecutor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ConcurrentSet;
import lombok.Getter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class ProxyServer {
    private String serviceKey;

    private Integer mappingPort;

    private ChannelFuture channelFuture;

    private volatile Channel proxyChannel;

    private Channel channel;

    private ForwardHandler forwardHandler;

    private EventLoopGroup boss;
    private EventLoopGroup worker;

    @Getter
    private Server server;

    public ProxyServer(String serviceKey, Integer mappingPort, Channel proxyChannel,  Server server) {
        this.serviceKey = serviceKey;
        this.mappingPort = mappingPort;
        this.proxyChannel = proxyChannel;
        this.forwardHandler = new ForwardHandler(serviceKey, proxyChannel, this);
        this.server = server;
    }

    public void send(Frame msg) {
        forwardHandler.send(msg);
    }

    public void open() {
        boss = new NioEventLoopGroup();
        worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap(); 

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                                String address = ((InetSocketAddress)(ctx.channel().remoteAddress())).toString();
                                forwardHandler.setChannel(address, ctx.channel());
                                Frame frame = new Frame(0x9, serviceKey, address, null);
                                forwardHandler.buildLatch(address);
                                proxyChannel.writeAndFlush(frame);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                String address = ((InetSocketAddress)(ctx.channel().remoteAddress())).toString();
                                Frame frame = new Frame(0xA, serviceKey, address, null);
                                forwardHandler.refuse(address);
                                forwardHandler.removeLatch(address);
                                forwardHandler.cleanDataChannel(address);
                                proxyChannel.writeAndFlush(frame);
                                ctx.fireChannelInactive();
                            }
                        });
                        //pipeline.addLast(new FixedLengthFrameDecoder(20));
                        pipeline.addLast(forwardHandler);
                    }
                });

        try {
            channelFuture = bootstrap.bind(mappingPort).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        channelFuture.addListener((future) -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }

        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public void close() {
        if (channel == null) {
            return;
        }
        channel.close();
    }

    public void closeChannel(Frame msg) {
        boolean closeChannel = forwardHandler.closeChannel(msg);
        forwardHandler.removeLatch(msg.getRealClientAddress());
    }

    public void shutdown() throws Exception {
        close();
        if (boss != null && worker != null) {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }
    }

    public ForwardHandler getForwardHandler() {
        return forwardHandler;
    }

    public void buildDataChannel(String address, Channel channel) {
        forwardHandler.buildDataChannel(address, channel);
    }

    public void forwardData(Channel channel2, ByteBuf msg) {
        forwardHandler.send(channel2, msg);
    }

    public void pass(String realClientAddress) {
        forwardHandler.pass(realClientAddress);
    }

    public void refuse(String realClientAddress) {
        forwardHandler.refuse(realClientAddress);
    }

    public void setProxyChannel(Channel channel2) {
        this.proxyChannel = channel2;
    }
    
}
