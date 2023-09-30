package com.github.wandererex.wormhole.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.ConfigLoader;
import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Proxy {
    public static CountDownLatch latch = new CountDownLatch(1);
    private String serverHost;

    private Integer serverPort;

    private ProxyServiceConfig config;

    private Channel channel;

    private ProxyClient client;

    public Proxy() throws Exception {
        String configPath = "/config.json";
        this.config = ConfigLoader.load(configPath);
        this.serverHost = config.getServerHost();
        this.serverPort = config.getServerPort();
        client = new ProxyClient(config);
    }

    public void start() throws Exception {
        channel = client.connect(serverHost, serverPort);
        online(channel);
        client.syncAuth();
        client.checkIdle();
        latch.await();
    }

    private void online(Channel channel) throws Exception {

        String string = ConfigLoader.serialize(config);
        InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress();
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        buffer.writeCharSequence(string, StandardCharsets.UTF_8);
        Frame frame = new Frame(0x1, null, localAddress.toString(), buffer);
        channel.writeAndFlush(frame);
    }

    public static void main(String[] args) throws Exception {
        Proxy proxy = new Proxy();
        proxy.start();
    }
}
