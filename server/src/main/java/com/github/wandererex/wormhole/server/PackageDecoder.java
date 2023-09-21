package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.FrameSerialization;
import com.github.wandererex.wormhole.serialize.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.List;

public class PackageDecoder extends MessageToMessageDecoder<ByteBuf> {
    private Serialization<Frame> serialization = new FrameSerialization();

    private ReleaseHandler releaseHandler = new ReleaseHandler();

    public PackageDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        ByteBuf copy = byteBuf.copy();
        AttributeKey<ByteBuf> attributeKey = AttributeKey.valueOf("buffer");
        Attribute<ByteBuf> attr = ctx.channel().attr(attributeKey);
        attr.set(copy);
        Frame pkg = serialization.deserialize(copy);
        out.add(pkg);
        ctx.pipeline().addLast(releaseHandler);
    }
}
