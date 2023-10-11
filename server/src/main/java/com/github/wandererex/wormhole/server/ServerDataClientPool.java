package com.github.wandererex.wormhole.server;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.Channel;

public class ServerDataClientPool {
    private static List<Channel> channels = new ArrayList<>();

    public synchronized static void revertClient(Channel channel) {
        channels.add(channel);
    }

    public synchronized static Channel getClient() {
        if (channels.size() == 0) {
            return null;
        }
        return channels.remove(0);
    }
}
