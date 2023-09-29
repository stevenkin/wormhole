package com.github.wandererex.wormhole.server;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class CommandHander extends SimpleChannelInboundHandler<Frame>{
    private AtomicInteger seq = new AtomicInteger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        if (msg.getOpCode() == 0xD) {
            String realClientAddress = msg.getRealClientAddress();
            String serviceKey = msg.getServiceKey();
            Frame frame = new Frame();
            frame.setOpCode(0xD1);
            frame.setRealClientAddress(realClientAddress);
            frame.setServiceKey(serviceKey);
            int andIncrement = seq.getAndIncrement();
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(4);
            buffer.writeInt(andIncrement);
            frame.setPayload(buffer);
            ctx.writeAndFlush(frame).addListener(f -> {
                if (f.isSuccess()) {
                    ctx.pipeline().remove(FrameDecoder.class);
                    ctx.pipeline().remove(FrameEncoder.class);
                    ctx.pipeline().remove(PackageDecoder.class);
                    ctx.pipeline().remove(PackageEncoder.class);
                    
                }
            });

        }
    }

}
