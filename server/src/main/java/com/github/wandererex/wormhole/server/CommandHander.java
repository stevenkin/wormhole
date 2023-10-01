package com.github.wandererex.wormhole.server;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.wandererex.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

@Sharable
@Slf4j
public class CommandHander extends SimpleChannelInboundHandler<Frame>{
    private DataForwardHander dataForwardHander;

    public CommandHander(DataForwardHander dataForwardHander) {
        this.dataForwardHander = dataForwardHander;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel {}", ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame msg) throws Exception {
        if (msg.getOpCode() == 0xD) {
            String realClientAddress = msg.getRealClientAddress();
            String serviceKey = msg.getServiceKey();
            dataForwardHander.setChannel(realClientAddress, serviceKey, ctx.channel());

            Frame frame = new Frame();
            frame.setOpCode(0xD1);
            frame.setRealClientAddress(realClientAddress);
            frame.setServiceKey(serviceKey);
            frame.setPayload(msg.getPayload());
            ctx.writeAndFlush(frame).addListener(f -> {
                if (f.isSuccess()) {
                    ctx.pipeline().remove(FrameDecoder.class);
                    ctx.pipeline().remove(FrameEncoder.class);
                    ctx.pipeline().remove(PackageDecoder.class);
                    ctx.pipeline().remove(PackageEncoder.class);
                    ctx.pipeline().addLast(dataForwardHander);
                }
            });

        }
    }

}