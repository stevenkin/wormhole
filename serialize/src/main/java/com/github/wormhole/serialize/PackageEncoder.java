package com.github.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

public class PackageEncoder extends MessageToMessageEncoder<Frame> {
    private Serialization<Frame> serialization = new FrameSerialization();

    public PackageEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame pkg, List<Object> out) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        ByteBuf buf = serialization.serialize(pkg, buffer);
        out.add(buf);
    }
}
