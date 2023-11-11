package com.github.wormhole.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.ack.AckHandler;
import com.github.wormhole.common.config.ProxyServiceConfig;
import com.github.wormhole.common.config.ProxyServiceConfig.ServiceConfig;
import com.github.wormhole.common.utils.RetryUtil;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.serialize.FrameDecoder;
import com.github.wormhole.serialize.FrameEncoder;
import com.github.wormhole.serialize.PackageDecoder;
import com.github.wormhole.serialize.PackageEncoder;
import com.github.wormhole.server.processor.SignalChannelContext;

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
    private ChannelFuture channelFuture;

    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private ProxyServiceConfig config;
    private Channel proxyChannel;
    private String proxyId;
    private Map<Integer, String> portServiceMap = new HashMap<>();

    private List<Channel> serverChannels = new ArrayList<>();

    private ClientHandler clientHandler;

    private Map<Channel, AckHandler> ackHandlerMap = new ConcurrentHashMap<>();

    private Server server;

    public ProxyServer(EventLoopGroup boss, EventLoopGroup worker, String proxyId, ProxyServiceConfig config, Channel channel, Server server) {
        this.boss = boss;
        this.worker = worker;
        this.config = config;
        this.proxyChannel = channel;
        this.proxyId = proxyId;
        this.server = server;
        this.clientHandler = new ClientHandler(this);
    }

    public void open() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        int port = ch.localAddress().getPort();
                        String string = portServiceMap.get(port);
                        AckHandler ackHandler = new AckHandler(ch, new SignalChannelContext(proxyChannel), proxyId, string, ch.remoteAddress().toString());
                        ackHandlerMap.put(ch, ackHandler);
                        pipeline.addLast(clientHandler);
                        pipeline.addLast(ackHandler);
                        pipeline.addLast(new LoggingHandler());
                    }
                });
        Map<String, ServiceConfig> map = config.getMap();
        for (Map.Entry<String, ServiceConfig> entry : map.entrySet()) {
            portServiceMap.put(entry.getValue().getMappingPort(), entry.getKey());
            Channel channel = bootstrap.bind(entry.getValue().getMappingPort()).sync().channel();
            serverChannels.add(channel);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serverChannels.forEach(Channel::close);
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public void close() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
    }

    public void sendToProxy(Frame frame) {
        RetryUtil.write(proxyChannel, frame);
    }

    public String getServiceKey(Integer port) {
        return portServiceMap.get(port);
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public EventLoopGroup getBoss() {
        return boss;
    }

    public EventLoopGroup getWorker() {
        return worker;
    }

    public ProxyServiceConfig getConfig() {
        return config;
    }

    public Channel getProxyChannel() {
        return proxyChannel;
    }

    public String getProxyId() {
        return proxyId;
    }

    public Map<Integer, String> getPortServiceMap() {
        return portServiceMap;
    }

    public List<Channel> getServerChannels() {
        return serverChannels;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public void refuse(String realClientAddress) {
        clientHandler.refuse(realClientAddress);
    }

    public Server getServer() {
        return server;
    }

    public Map<Channel, AckHandler> getAckHandlerMap() {
        return ackHandlerMap;
    }
    
}
