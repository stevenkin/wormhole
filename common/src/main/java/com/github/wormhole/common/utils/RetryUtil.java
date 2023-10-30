package com.github.wormhole.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;

public class RetryUtil {

    public static <T> void write( Channel channel, T msg) {
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    channel.writeAndFlush(msg).addListener(holder.t);
                }
            };
            holder.t = listener;
            channel.writeAndFlush(msg).addListener(holder.t);
    }
}
