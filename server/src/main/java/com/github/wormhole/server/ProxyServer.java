package com.github.wormhole.server;

import com.github.wormhole.common.config.ProxyServiceConfig;
import com.github.wormhole.serialize.FrameDecoder;
import com.github.wormhole.serialize.FrameEncoder;
import com.github.wormhole.serialize.PackageDecoder;
import com.github.wormhole.serialize.PackageEncoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ProxyServer {
    private int port;

    private ChannelFuture channelFuture;

    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private ProxyServiceConfig config;
    private Channel proxyChannel;
    private String proxyId;

    public ProxyServer(EventLoopGroup boss, EventLoopGroup worker, String proxyId, ProxyServiceConfig config, Channel channel) {
        this.port = port;
        this.boss = boss;
        this.worker = worker;
        this.config = config;
        this.proxyChannel = channel;
        this.proxyId = proxyId;
    }

    public void open() {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ClientHandler());
                        pipeline.addLast(new LoggingHandler());
                    }
                });

        try {
            channelFuture = bootstrap.bind(port).sync();
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
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
    }
}
