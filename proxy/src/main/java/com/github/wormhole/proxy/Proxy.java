package com.github.wormhole.proxy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.Context;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.SignalClient;
import com.github.wormhole.common.config.ProxyServiceConfig;
import com.github.wormhole.common.utils.ConfigLoader;
import com.github.wormhole.common.utils.Connection;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.common.utils.RetryUtil;
import com.github.wormhole.proxy.processor.DataChannelProcessor;
import com.github.wormhole.proxy.processor.DataTransAckProcessor;
import com.github.wormhole.proxy.processor.DisconnectClientProcessor;
import com.github.wormhole.proxy.processor.ProxyRegisterAckProcessor;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class Proxy implements Context{
    private static Proxy proxy;

    private String serverHost;

    private Integer serverPort;

    private ProxyServiceConfig config;

    private Channel channel;

    private SignalClient signalClient;

    private DataClientPool dataClientPool;

    private String proxyId;

    private DataTransAckProcessor dataTransAckProcessor;

    private DataChannelProcessor dataChannelProcessor;

    public Proxy() throws Exception {
        String configPath = "/config.json";
        this.config = ConfigLoader.load(configPath);
        this.serverHost = config.getServerHost();
        this.serverPort = config.getServerPort();
        this.dataClientPool = new DataClientPool(serverHost, config.getDataTransPort(), 1, this);
        this.signalClient = new SignalClient(serverHost, serverPort, this);
    }

    public void start() throws Exception {
        this.dataTransAckProcessor = new DataTransAckProcessor(this);
        this.dataChannelProcessor = new DataChannelProcessor(this);
        signalClient.register(dataChannelProcessor)
            .register(dataTransAckProcessor)
            .register(new DisconnectClientProcessor(this))
            .register(new ProxyRegisterAckProcessor(this));
        channel = signalClient.connect();
        online(channel);
        dataClientPool.init();
    }

    private void online(Channel channel) {
        String string = ConfigLoader.serialize(config);
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        buffer.writeCharSequence(string, StandardCharsets.UTF_8);
        Frame frame = new Frame();
        frame.setOpCode(0x1);
        frame.setPayload(buffer);
        frame.setRequestId(IDUtil.genRequestId());
        RetryUtil.writeLimitTime(new Connection() {
            @Override
            public ChannelFuture write(Object msg) {
                return channel.writeAndFlush(msg);
            }
        }, frame, 3);
    }

    public static void main(String[] args) throws Exception {
        proxy = new Proxy();
        proxy.start();
    }

    public String getServerHost() {
        return serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public ProxyServiceConfig getConfig() {
        return config;
    }

    public Channel getChannel() {
        return channel;
    }

    public SignalClient getSignalClient() {
        return signalClient;
    }

    public DataClientPool getDataClientPool() {
        return dataClientPool;
    }

    public String getProxyId() {
        return proxyId;
    }

    @Override
    public void write(Object msg) {
        signalClient.send((Frame) msg);
    }

    @Override
    public Object read() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public String id() {
        return proxyId;
    }

    public void setProxyId(String id) {
        this.proxyId = id;
    }

    public static Proxy getProxy() {
        return proxy;
    }

    public DataTransAckProcessor getDataTransAckProcessor() {
        return dataTransAckProcessor;
    }

    public DataChannelProcessor getDataChannelProcessor() {
        return dataChannelProcessor;
    }
    

}
