package com.github.wandererex.wormhole.server;

import com.github.wandererex.wormhole.serialize.Frame;
import com.github.wandererex.wormhole.serialize.FrameSerialization;
import com.github.wandererex.wormhole.serialize.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

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
