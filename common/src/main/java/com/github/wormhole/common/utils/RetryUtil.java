package com.github.wormhole.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;

public class RetryUtil {

    public static <T> void write( Channel channel, T msg) {
            Holder<GenericFutureListener> holder = new Holder<>();
            GenericFutureListener listener = f -> {
                Object obj = msg;
                if (!f.isSuccess()) {
                    Thread.sleep(100);
                    channel.writeAndFlush(obj).addListener(holder.t);
                }
            };
            holder.t = listener;
            channel.writeAndFlush(msg).addListener(holder.t);
    }

    public static <T> void writeLimitTime(Connection conn, T msg, int num) {
            Holder<GenericFutureListener> holder = new Holder<>();
            Holder<Integer> holder2 = new Holder<>();
            holder2.t = num;
            GenericFutureListener listener = f -> {
                if (!f.isSuccess() && holder2.t > 0) {
                    Thread.sleep(100);
                    ChannelFuture write = conn.write(msg);
                    if (write != null) {
                        write.addListener(holder.t);
                    }
                    holder2.t--;
                }
            };
            holder.t = listener;
            ChannelFuture write = conn.write(msg);
                    if (write != null) {
                        write.addListener(holder.t);
                    }
            holder2.t--;
    }

    public static <T> void writeLimitNumThen(Connection conn, T msg, int num, Runnable runnable) {
        Holder<GenericFutureListener> holder = new Holder<>();
            Holder<Integer> holder2 = new Holder<>();
            holder2.t = num;
            GenericFutureListener listener = f -> {
                if (!f.isSuccess() && holder2.t > 0) {
                    Thread.sleep(100);
                    ChannelFuture write = conn.write(msg);
                    if (write != null) {
                        write.addListener(holder.t);
                    }
                    holder2.t--;
                } else {
                    runnable.run();
                }
            };
            holder.t = listener;
            ChannelFuture write = conn.write(msg);
                    if (write != null) {
                        write.addListener(holder.t);
                    }
            holder2.t--;
    }

}
