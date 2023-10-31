package com.github.wormhole.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.github.wormhole.common.utils.RetryUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignalClient extends Client<Frame>{
    private Map<String, ChannelPromise> reqMap = new ConcurrentHashMap<>();

    private SignalHandler signalHandler = new SignalHandler();

    public SignalClient(String ip, Integer port) {
        super(ip, port);
    }

    public SignalClient register(Processor signalProcessor) {
        this.signalHandler.register(signalProcessor);
        return this;
    }

    @Override
    public void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(signalHandler);
    }

    @Override
    public  ChannelFuture send(Frame msg) {
        String key = System.currentTimeMillis() + RandomStringUtils.randomAlphabetic(8);
        msg.setRequestId(key);
        ChannelPromise newPromise = channel.newPromise();
        if (key != null) {
             reqMap.put(key, newPromise);
        }
        RetryUtil.write(channel, msg);
        return newPromise;
    }
    
}
