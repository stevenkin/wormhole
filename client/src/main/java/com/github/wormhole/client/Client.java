package com.github.wormhole.client;

import com.github.wormhole.serialize.NetworkUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Client {
    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private String ip;

    private Integer port;

    private Channel channel;

    public Client(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
        this.clientBootstrap = new Bootstrap();
        this.clientGroup = new NioEventLoopGroup();
        clientBootstrap.group(clientGroup).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        //初始化时将handler设置到ChannelPipeline
                        @Override
                        public void initChannel(SocketChannel ch) {
                            initChannelPipeline(ch.pipeline());
                        }
                    });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    protected abstract void initChannelPipeline(ChannelPipeline pipeline);

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
    }

    public void reconnect() throws Exception {
        disconnect();
        connect(ip, port);
    }

    public void shutdown() throws Exception {
        disconnect();
        clientGroup.shutdownGracefully().syncUninterruptibly();
    }

    

    
}
