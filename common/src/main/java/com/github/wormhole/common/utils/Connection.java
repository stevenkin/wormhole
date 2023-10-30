package com.github.wormhole.common.utils;

import io.netty.channel.ChannelFuture;

public interface Connection {
    ChannelFuture write(Object msg);
}
