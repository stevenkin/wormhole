package com.github.wormhole.client.ack;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.client.Context;
import com.github.wormhole.common.utils.IDUtil;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class AckHandler extends ChannelDuplexHandler{
    private long writeByteCount;

    private long readByteCount;

    private Context context;

    private String proxyId;

    private String serviceKey;

    private boolean isServerSide;

    public AckHandler(boolean isServerSide) {
        this.isServerSide = isServerSide;
    }

    public AckHandler(Context context, String proxyId, String serviceKey, boolean isServerSide) {
        this.context = context;
        this.proxyId = proxyId;
        this.serviceKey = serviceKey;
        this.isServerSide = isServerSide;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
        ByteBuf buf = (ByteBuf) msg;
        readByteCount += buf.readableBytes();
        Frame frame = new Frame();
        frame.setOpCode(0x3);
        frame.setRequestId(IDUtil.genRequestId());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        JSONObject jsonObject = new JSONObject();
        Channel channel = ctx.channel();
        String channelId = null;
        if (isServerSide) {
            channelId = channel.remoteAddress().toString() + "-" + channel.localAddress().toString();
        } else {
            channelId = channel.localAddress().toString() + "-" + channel.remoteAddress().toString();
        }
        jsonObject.put("channelId", channelId);
        jsonObject.put("ackSize", readByteCount);
        String jsonString = jsonObject.toJSONString();
        buffer.writeCharSequence(jsonString, Charset.forName("UTF-8"));
        buffer.writeLong(readByteCount);
        frame.setPayload(buf);
        context.write(frame);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
        ByteBuf buf = (ByteBuf) msg;
        writeByteCount += buf.readableBytes();
    }

    public void reflush(Context context, String proxyId, String serviceKey) {
        this.context = context;
        this.proxyId = proxyId;
        this.serviceKey = serviceKey;
        this.readByteCount = 0;
        this.writeByteCount = 0;
    }
}
