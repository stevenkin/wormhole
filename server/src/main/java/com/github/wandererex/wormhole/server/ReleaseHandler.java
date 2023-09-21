package com.github.wandererex.wormhole.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class ReleaseHandler extends SimpleChannelInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        AttributeKey<ByteBuf> attributeKey = AttributeKey.valueOf("buffer");
        Attribute<ByteBuf> attr = ctx.channel().attr(attributeKey);
        ByteBuf byteBuf = attr.get();
        if (byteBuf != null) {
            log.info("ReleaseHandler {}", byteBuf);
            ReferenceCountUtil.release(msg);
        }
    }
    
}
