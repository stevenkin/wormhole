package com.github.wormhole.client.ack;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.Context;
import com.github.wormhole.client.DataClient;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AckHandler extends ChannelDuplexHandler{
    private long writeByteCount;

    private long readByteCount;

    private long ackCount;

    private Context context;

    private String proxyId;

    private String serviceKey;

    private Channel channel;

    private String peerClientAddress;

    private ChannelPromise promise;

    public AckHandler(Channel channel,Context context, String proxyId, String serviceKey, String peerClientAddress) {
        this.context = context;
        this.proxyId = proxyId;
        this.serviceKey = serviceKey;
        this.channel = channel;
        this.peerClientAddress = peerClientAddress;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        readByteCount += buf.readableBytes();
        log.info("收到{}数据{}", peerClientAddress, readByteCount);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
        promise.addListener(f -> {
            if (f.isSuccess()) {
                ByteBuf buf = (ByteBuf) msg;
                writeByteCount += buf.readableBytes();
                Frame frame = new Frame();
                frame.setOpCode(0x3);
                frame.setRequestId(IDUtil.genRequestId());
                frame.setServiceKey(serviceKey);
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("channelId", peerClientAddress);
                jsonObject.put("ackSize", writeByteCount);
                String jsonString = jsonObject.toJSONString();
                buffer.writeCharSequence(jsonString, Charset.forName("UTF-8"));
                frame.setPayload(buffer);
                context.write(frame);
                log.info("发送给{}数据{}", peerClientAddress, writeByteCount);
            }
        });

    }

    public void setAck(long ack) {
        channel.eventLoop().submit(() -> {
            AckHandler.this.ackCount = ack;
            if (AckHandler.this.ackCount == AckHandler.this.readByteCount) {
                if (promise != null) {
                    promise.setSuccess();
                } 
            }
        });
    }

    public boolean isAckComplate() {
        return ackCount == readByteCount;
    }

    public void setPromise(ChannelPromise promise) {
        this.promise = promise;
    }

    public void setPeerClientAddress(String peerClientAddress) {
        this.peerClientAddress = peerClientAddress;
    }

    public void clear() {
        this.writeByteCount = 0;
        this.readByteCount = 0;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
