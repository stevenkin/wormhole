package com.github.wormhole.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;

public class RetryUtil {

    public static <T> void write( Channel channel, T msg) {
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                if (!f.isSuccess()) {
                    Thread.sleep(100);
                    channel.writeAndFlush(msg).addListener(holder.t);
                }
            };
            holder.t = listener;
            channel.writeAndFlush(msg).addListener(holder.t);
    }

    public static <T> void writeLimitNum(Connection conn, T msg, int num) {
            Holder<GenericFutureListener> holder = new Holder<>();
            Holder<Integer> holder2 = new Holder<>();
            holder2.t = num;
            GenericFutureListener listener = f -> {
                if (!f.isSuccess() && holder2.t > 0) {
                    Thread.sleep(100);
                    conn.write(msg).addListener(holder.t);
                    holder2.t--;
                }
            };
            holder.t = listener;
            conn.write(msg).addListener(holder.t);
            holder2.t--;
    }

}