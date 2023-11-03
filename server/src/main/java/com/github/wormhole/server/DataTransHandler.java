package com.github.wormhole.server;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSONObject;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class DataTransHandler extends ChannelInboundHandlerAdapter{
    private Map<String, Channel> channalMap = new ConcurrentHashMap<>();

    private Map<Channel, Channel> clientChannelMap = new ConcurrentHashMap<>();

    private Map<String, AtomicLong> dataCount = new ConcurrentHashMap<>();

    private Server server;

    public DataTransHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        Channel channel = ctx.channel();
        String key = channel.remoteAddress().toString() + "-" + channel.localAddress().toString();
        channalMap.put(key, channel);
        dataCount.put(channel.id().toString(), new AtomicLong(0));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        Channel channel = ctx.channel();
        channalMap.remove(channel.id().toString());
        dataCount.remove(channel.id().toString());
    }

    public Channel getDataTransChannel(String channelId) {
        return channalMap.get(channelId);
    }

    public void buildDataClientChannelMap(Channel dataChannel, Channel clientChannel) {
        clientChannelMap.put(dataChannel, clientChannel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        Channel channel2 = clientChannelMap.get(channel);
        channel2.writeAndFlush(msg);
        AtomicLong long1 = dataCount.get(channel.id().toString());
        if (long1 != null) {
            long num = long1.addAndGet(((ByteBuf) msg).readableBytes());
            String string = server.getDataChannelProxyIdMap().get(channel.id().toString());
            if (string != null) {
                 Channel channel3 = server.getProxyIdChannelMap().get(string);   
                 Frame frame = new Frame();
                 frame.setOpCode(0x3);
                 frame.setProxyId(string);   
                 ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                 JSONObject jsonObject = new JSONObject();
                 jsonObject.put("channelId", channel.id().toString());
                 jsonObject.put("ackSize", num);
                 String jsonString = jsonObject.toJSONString();
                 buffer.writeCharSequence(jsonString, Charset.forName("UTF-8"));
                 frame.setPayload(buffer);
                 channel3.writeAndFlush(frame);
            }
        }
    }

    public void clear(String clientAddress) {
        
    }
}
