package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ProxyServer {
    private int port;

    private ChannelFuture channelFuture;
    public ProxyServer(int port) {
        this.port = port;
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
                        pipeline.addLast(new SimpleChannelInboundHandler<Frame>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
                                System.out.println("read: " + msg);
                                if (msg.getOpCode() == 0x1) {
                                    msg.setOpCode(0x11);
                                    ctx.writeAndFlush(msg);
                                    System.out.println("write: " + msg);
                                }
                                if (msg.getOpCode() == 0x5) {
                                    Frame frame = new Frame(0x6, null, null, null);
                                    ctx.writeAndFlush(frame);
                                    System.out.println("write: " + frame);
                                }
                            }
                        });
                        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
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

    // public static void main(String[] args) {
    //     new ProxyServer(8090).open();
    // }
}
