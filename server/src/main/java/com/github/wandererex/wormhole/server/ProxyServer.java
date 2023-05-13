package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ProxyServer {
    private String serviceKey;

    private Integer mappingPort;

    private ChannelFuture channelFuture;

    private Channel proxyChannel;

    private ForwardHandler forwardHandler;

    public ProxyServer(String serviceKey, Integer mappingPort, Channel proxyChannel) {
        this.serviceKey = serviceKey;
        this.mappingPort = mappingPort;
        this.proxyChannel = proxyChannel;
        this.forwardHandler = new ForwardHandler(serviceKey, proxyChannel);
    }

    public void send(byte[] buf) {
        forwardHandler.send(buf);
    }

    public void open() {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new FixedLengthFrameDecoder(20));
                        pipeline.addLast(forwardHandler);
                        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
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
}
