package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.RandomStringUtils;

@Slf4j
public class DataClient {
    private Bootstrap clientBootstrap;

    private NioEventLoopGroup clientGroup;

    private String ip;

    private Integer port;

    private Channel channel;

    private Channel channel2;

    private long lastHeatbeatTime;

    private boolean isTaked;

    private boolean cache;

    @Getter
    private Map<String, ChannelPromise> reqMap = new ConcurrentHashMap<>();

    @Getter
    private DataClientHandler dataClientHandler = new DataClientHandler();

    @Getter
    private ProxyClient proxyClient;

    @Setter
    @Getter
    private ChannelPromise channelPromise;

    public DataClient(Channel proxyServerChannel) {
        this.channel2 = proxyServerChannel;
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
                            ch.pipeline().addLast(new DataClientCmdHandler(DataClient.this));
                            ch.pipeline().addLast(new LoggingHandler());
                        }
                    });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clientGroup.shutdownGracefully().syncUninterruptibly();
        }));
    }

    public synchronized ChannelPromise take(String serviceKey, String address) {
        if (isTaked) {
            throw new RuntimeException("dataclient is taked");
        }
        ChannelPromise send = channel.newPromise();
        send.setSuccess();
        if (!cache) {
            Frame frame = new Frame(0xD, serviceKey, address, null);
            send = send(frame, channel);
            send.addListener(f -> {
                if (f.isSuccess()) {
                    synchronized(DataClient.this) {
                        channel.pipeline().remove(FrameDecoder.class);
                        channel.pipeline().remove(FrameEncoder.class);
                        channel.pipeline().remove(PackageDecoder.class);
                        channel.pipeline().remove(PackageEncoder.class);
                        channel.pipeline().remove(DataClientCmdHandler.class);
                        channel.pipeline().remove(LoggingHandler.class);
                        channel.pipeline().addLast(dataClientHandler);
                        channel.pipeline().addLast(new LoggingHandler());
                        isTaked = true;
                    }
                }
            });
        }
        return send;
    }

    public synchronized ChannelPromise revert(String serviceKey, String address) {
        if (!isTaked) {
            return channel2.newPromise().setFailure(new RuntimeException("dataclient is not taked"));
        }
        Frame frame = new Frame(0xC, serviceKey, address, null);
        ChannelPromise send = send(frame, channel2);
        send.addListener(f -> {
            if (f.isSuccess()) {
                synchronized(DataClient.this) {
                    channel.pipeline().remove(DataClientHandler.class);
                    channel.pipeline().remove(LoggingHandler.class);
                    channel.pipeline().addLast(new FrameDecoder());
                    channel.pipeline().addLast(new FrameEncoder());
                    channel.pipeline().addLast(new PackageDecoder());
                    channel.pipeline().addLast(new PackageEncoder());
                    channel.pipeline().addLast(new DataClientCmdHandler(DataClient.this));
                    channel.pipeline().addLast(new LoggingHandler());
                    isTaked = false;
                    cache = false;
                    proxyClient = null;
                }
            }
        });
        return send;
    }

    public synchronized ChannelPromise cache(String serviceKey, String address) {
        if (!isTaked) {
            throw new RuntimeException("dataclient is not taked");
        }
        Frame frame = new Frame(0xC2, serviceKey, address, null);
        ChannelPromise send = send(frame, channel2);
        send.addListener(f -> {
            if (f.isSuccess()) {
                proxyClient = null;
                isTaked = false;
                cache = true;
            }
        });
        return send;
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
    }

    public void reconnect() throws Exception {
        disconnect();
        connect(ip, port);
    }

    public ChannelPromise send(Frame frame, Channel channel2) {
        String key = System.currentTimeMillis() + RandomStringUtils.randomAlphabetic(8);
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        buffer.writeCharSequence(key, Charset.forName("UTF-8"));
        frame.setPayload(buffer);
        ByteBuf payload = frame.getPayload();
        String string = key;
        payload.markReaderIndex();
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f1 -> {
                if (!f1.isSuccess()) {
                    ChannelPipeline pipeline = channel2.pipeline();
                    pipeline.forEach(e -> log.info("{} -> {}", e.getKey(), e.getValue()));
                    channel2.writeAndFlush(frame).addListener(holder.t);;
                }
            };
            holder.t = listener;
            ChannelPromise newPromise = channel2.newPromise();
            if (string != null) {
                reqMap.put(string, newPromise);
            }
            channel2.writeAndFlush(frame).addListener(holder.t);
            return newPromise;
    }

    public ChannelPromise send(ByteBuf byteBuf) {
        if (channelPromise == null) {
            throw new UnsupportedOperationException();
        }
        ChannelPromise newPromise = channel.newPromise();
        channelPromise.addListener(f -> {
            channel.writeAndFlush(byteBuf, newPromise);
        });
        return newPromise;
    }

    public void shutdown() throws Exception {
        disconnect();
        clientGroup.shutdownGracefully().syncUninterruptibly();
    }

    public void updateHeatbeatTime() {
        lastHeatbeatTime = System.currentTimeMillis();
    }

    public void checkIdle() {
        clientGroup.scheduleWithFixedDelay(() -> {
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            Frame frame = new Frame(0x5, null, remoteAddress.toString(), null);
            channel.writeAndFlush(frame);
        }, 0, 5, TimeUnit.SECONDS);
        clientGroup.scheduleWithFixedDelay(() -> {
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

    public void setProxyClient(ProxyClient proxyClient) {
        this.proxyClient = proxyClient;
        this.dataClientHandler.setProxyClient(proxyClient);
    }

}
