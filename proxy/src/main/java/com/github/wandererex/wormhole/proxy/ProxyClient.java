package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxyClient {
    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private Channel channel;

    private volatile boolean connectSuccess = false;

    private volatile boolean authSuccess = false;

    private long lastHeatbeatTime;

    private ChannelPromise channelPromise;

    public ProxyClient(ProxyServiceConfig config) {
        this.clientBootstrap = new Bootstrap();
        this.clientGroup = new NioEventLoopGroup();

        clientBootstrap.group(clientGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    //初始化时将handler设置到ChannelPipeline
                    @Override
                    public void initChannel(SocketChannel ch) {
                        //ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(10 * 3, 15 * 3, 20 * 3));
                        ch.pipeline().addLast(new FrameDecoder());
                        ch.pipeline().addLast(new FrameEncoder());
                        ch.pipeline().addLast(new PackageDecoder());
                        ch.pipeline().addLast(new PackageEncoder());
                        ch.pipeline().addLast(new ProxyHandler(ProxyClient.this, config));
                        ch.pipeline().addLast("logs", new LoggingHandler(LogLevel.DEBUG));
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Proxy.latch.countDown();;
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public Channel connect(String ip, int port) throws Exception {
        /**
         * 最多尝试5次和服务端连接
         */
        this.channel = doConnect(ip, port, 5);
        this.channelPromise = new DefaultChannelPromise(this.channel);
        this.connectSuccess = true;
        return this.channel;
    }

    private Channel doConnect(String ip, int port, int retry) throws InterruptedException {
        ChannelFuture future = null;
        for (int i = retry; i > 0; i--) {
            try {
                future = clientBootstrap.connect(ip, port).sync();
            } catch (InterruptedException e) {
                log.debug("debug:connect business server fail, client " + NetworkUtil.getLocalHost() + ", server " + ip + ":" + port);
            }
            if (future.isSuccess()) {
                return future.channel();
            }
            Thread.sleep(5000);
        }
        throw new RuntimeException("connect business server fail, client " + NetworkUtil.getLocalHost() + ", server " + ip + ":" + port);
    }

    public void disconnect() throws Exception {
        if (!connectSuccess) {
            log.error("no connect!");
            return;
        }
        channel.close().sync();
        connectSuccess = false;
    }

    public void send(Frame frame) throws Exception {
        if (!connectSuccess) {
            throw new RuntimeException("no connect!");
        }
        channel.writeAndFlush(frame).sync();
    }

    public void authSuccess() {
        this.authSuccess = true;
        channelPromise.setSuccess();
    }

    public void shutdown() throws Exception {
        disconnect();
        clientGroup.shutdownGracefully().syncUninterruptibly();
        Proxy.latch.countDown();
    }

    public void updateHeatbeatTime() {
        lastHeatbeatTime = System.currentTimeMillis();
    }

    public void checkIdle() {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (lastHeatbeatTime > 0) {
                if (System.currentTimeMillis() - lastHeatbeatTime > 15000) {
                    try {
                        shutdown();
                    } catch (Exception e) {
                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            Frame frame = new Frame(0x5, null, null);
            channel.writeAndFlush(frame);
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void syncAuth() throws InterruptedException {
        channelPromise.sync();
    }

}
