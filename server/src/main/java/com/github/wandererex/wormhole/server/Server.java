package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;

public class Server {
    private int port;

    private DataForwardHander dataForwardHander;

    @Getter
    private CommandHander commandHander;

    private ChannelFuture channelFuture;

    @Getter
    private ProxyServerHandler proxyServerHandler;

    private AuthHandler authHandler;

    public Server(int port, String redisIp, Integer redisPort) {
        this.port = port;
        this.proxyServerHandler = new ProxyServerHandler(this);
        this.dataForwardHander = new DataForwardHander(proxyServerHandler);
        this.commandHander = new CommandHander(dataForwardHander);
        this.authHandler = new AuthHandler(redisIp, redisPort);
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
                        pipeline.addLast(new FrameDecoder());
                        pipeline.addLast(new FrameEncoder());
                        pipeline.addLast(new PackageDecoder());
                        pipeline.addLast(new PackageEncoder());
                        pipeline.addLast(proxyServerHandler);
                        pipeline.addLast(commandHander);
                        pipeline.addLast(authHandler);
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

    public static void main(String[] args) {
        String port = null;
        String redisIp = null;
        Integer redisPort = null;
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--port")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            port = arg;
                        }
                    }
                } else if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--redisIp")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            redisIp = arg;
                        }
                    }
                } else if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--redisPort")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            redisPort = Integer.parseInt(arg);
                        }
                    }
                }
            }
            if (port != null  && redisPort != null && StringUtils.isNotEmpty(redisIp)) {
                new Server(Integer.parseInt(port), redisIp, redisPort).open();
            }
        }
    }

    public void removeProxyServer(String serviceKey) {
        proxyServerHandler.remove(serviceKey);
    }
}
