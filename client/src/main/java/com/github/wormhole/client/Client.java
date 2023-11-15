package com.github.wormhole.client;

import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.common.utils.NetworkUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Client<T> {
    private String id;

    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private String ip;

    private Integer port;

    protected Channel channel;

    private Context context;

    public Client(String ip, Integer port, Context context) {
        this.ip = ip;
        this.port = port;
        this.context = context;
        this.clientBootstrap = new Bootstrap();
        this.clientGroup = new NioEventLoopGroup();
        clientBootstrap.group(clientGroup)
        .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.ALLOW_HALF_CLOSURE, true)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        //初始化时将handler设置到ChannelPipeline
                        @Override
                        public void initChannel(SocketChannel ch) {
                            initChannelPipeline(ch.pipeline());
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        }
                    });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            channel.close();
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public abstract void initChannelPipeline(ChannelPipeline pipeline);

    public abstract ChannelFuture send(T msg);

    public Channel connect() throws Exception {
        /**
         * 最多尝试5次和服务端连接
         */
        this.channel = doConnect(ip, port, 5);
        this.id = this.channel.id().toString();
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
        connect();
    }

    public void shutdown() throws Exception {
        disconnect();
        clientGroup.shutdownGracefully().syncUninterruptibly();
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

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getId() {
        return channel.localAddress().toString() + "-" + channel.remoteAddress().toString();
    }

    public Context getContext() {
        return context;
    }
    
}
