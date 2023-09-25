package com.github.wandererex.wormhole.server;

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
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataClient {
    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private String ip;

    private Integer port;

    private Channel channel;

    public DataClient(ProxyServiceConfig config) {
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
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<Frame>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
                                    if (msg.getOpCode() == 0xD1) {
                                        int seq = msg.getPayload().readInt();
                                    }
                                }
                                
                            });
                        }
                    });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public void init(String serviceKey, String client) {
        Frame frame = new Frame();
        frame.setOpCode(0x9);
        frame.setRealClientAddress(client);
        frame.setServiceKey(serviceKey);
        channel.writeAndFlush(frame).syncUninterruptibly();
    }
    

    public Channel connect(String ip, int port) throws Exception {
        /**
         * 最多尝试5次和服务端连接
         */
        this.channel = doConnect(ip, port, 5);
        this.ip = ip;
        this.port = port;
        return channel;
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
        channel.close().sync();
        connectSuccess = false;
    }

    public void reconnect() throws Exception {
        disconnect();
        connect(ip, port);
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
        scheduledExecutorService.shutdown();
        clientGroup.shutdownGracefully().syncUninterruptibly();
        Proxy.latch.countDown();
    }

    public void updateHeatbeatTime() {
        lastHeatbeatTime = System.currentTimeMillis();
    }

    public void checkIdle() {
        scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            Frame frame = new Frame(0x5, null, remoteAddress.toString(), null);
            channel.writeAndFlush(frame);
        }, 0, 5, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (lastHeatbeatTime > 0) {
                if (System.currentTimeMillis() - lastHeatbeatTime > 15000) {
                    log.info("reconnect");
                    for (;;) {
                        try {
                            reconnect();
                            break;
                        } catch (Exception e) {
                            log.info("reconnect error {}", e);
                        }
                    }
                }
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    public void syncAuth() throws InterruptedException {
        channelPromise.sync();
    }

    public static org.slf4j.Logger getLog() {
        return log;
    }

    public Bootstrap getClientBootstrap() {
        return clientBootstrap;
    }

    public NioEventLoopGroup getClientGroup() {
        return clientGroup;
    }

    public Channel getChannel() {
        return channel;
    }

    public Channel getChannel1() {
        return channel1;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public boolean isConnectSuccess() {
        return connectSuccess;
    }

    public boolean isAuthSuccess() {
        return authSuccess;
    }

    public long getLastHeatbeatTime() {
        return lastHeatbeatTime;
    }

    public ChannelPromise getChannelPromise() {
        return channelPromise;
    }

    public void setClientBootstrap(Bootstrap clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    public void setClientGroup(NioEventLoopGroup clientGroup) {
        this.clientGroup = clientGroup;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }


    public void setConnectSuccess(boolean connectSuccess) {
        this.connectSuccess = connectSuccess;
    }

    public void setAuthSuccess(boolean authSuccess) {
        this.authSuccess = authSuccess;
    }

    public void setLastHeatbeatTime(long lastHeatbeatTime) {
        this.lastHeatbeatTime = lastHeatbeatTime;
    }

    public void setChannelPromise(ChannelPromise channelPromise) {
        this.channelPromise = channelPromise;
    }

    public String getRealAddress() {
        return realAddress;
    }

    public void setRealAddress(String realAddress) {
        this.realAddress = realAddress;
    }

    

}
