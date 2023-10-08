package com.github.wandererex.wormhole.server;

import java.nio.charset.Charset;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import redis.clients.jedis.Jedis;

@Sharable
public class AuthHandler extends SimpleChannelInboundHandler<Frame>{
    private String ip;
    private Integer port;

    public AuthHandler(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        if (msg.getOpCode() == 0x0) {
            Jedis jedis = new Jedis(ip, port);
            ByteBuf payload = msg.getPayload();
            String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
            JSONObject parseObject = JSON.parseObject(string);
            String username = parseObject.getString("username");
            String password = parseObject.getString("password");
            String string2 = jedis.get(username);
            Frame frame = null;
            if (StringUtils.isNoneBlank(password) && StringUtils.isNoneBlank(string2) && password.equals(string2)) {
                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
                String val = Long.toString(System.currentTimeMillis()) + Integer.toString(new Random().nextInt());
                buffer.writeCharSequence(val, Charset.forName("UTF-8"));
                frame = new Frame(0x01, null, null, buffer);
            } else {
                frame = new Frame(0x00, null, null, null);
            }
            ctx.writeAndFlush(frame);
            ctx.pipeline().remove(this);
            jedis.close();
        }
    }
    
}
