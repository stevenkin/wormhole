package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxyClient {
    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private Channel channel;

    private Channel channel1;

    private String serviceKey;

    private volatile boolean connectSuccess = false;

    private volatile boolean authSuccess = false;

    private long lastHeatbeatTime;

    private ChannelPromise channelPromise;

    public ProxyClient(ProxyServiceConfig config) {
        this.clientBootstrap = new Bootstrap();
        this.clientGroup = new NioEventLoopGroup();
        if (config == null) {
            clientBootstrap.group(clientGroup).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        //初始化时将handler设置到ChannelPipeline
                        @Override
                        public void initChannel(SocketChannel ch) {
                            //ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(10 * 3, 15 * 3, 20 * 3));
                            //ch.pipeline().addLast(new FixedLengthFrameDecoder(20));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    Frame frame = new Frame(0xB, serviceKey, null);
                                    channel1.writeAndFlush(frame);
                                    ctx.fireChannelInactive();
                                }
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    if (channel1 != null) {
                                        byte[] bytes = new byte[msg.readableBytes()];
                                        msg.readBytes(bytes, 0, bytes.length);
                                        List<Frame> frames = NetworkUtil.byteArraytoFrameList(bytes, serviceKey);
                                        log.info("proxy read from service data {}", bytes);
                                        for (Frame frame : frames) {
                                            TaskExecutor.get().addTask(new Task(channel1, frame));
                                        }
                                    }
                                }
                            });
                            ch.pipeline().addLast("logs", new LoggingHandler(LogLevel.ERROR));
                        }
                    });
        }
        else {
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
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Proxy.latch.countDown();
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public void setChannel1(Channel channel1) {
        this.channel1 = channel1;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
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
