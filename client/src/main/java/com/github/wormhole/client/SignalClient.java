package com.github.wormhole.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.github.wormhole.serialize.Frame;
import com.github.wormhole.serialize.Holder;

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

    public SignalClient(String ip, Integer port) {
        super(ip, port);
    }

    @Override
    protected void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new SignalHandler());
    }

    @Override
    protected  ChannelFuture send(Frame msg) {
        String key = System.currentTimeMillis() + RandomStringUtils.randomAlphabetic(8);
        msg.setSessionId(key);
        Holder<GenericFutureListener> holder = new Holder<>();
        GenericFutureListener listener = f1 -> {
            if (!f1.isSuccess()) {
                channel.writeAndFlush(msg).addListener(holder.t);;
            }
        };
        holder.t = listener;
        ChannelPromise newPromise = channel.newPromise();
        if (key != null) {
             reqMap.put(key, newPromise);
        }
        channel.writeAndFlush(msg).addListener(holder.t);
        return newPromise;
    }
    
}
