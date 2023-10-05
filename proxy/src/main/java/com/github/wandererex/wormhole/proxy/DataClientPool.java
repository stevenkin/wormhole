package com.github.wandererex.wormhole.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import lombok.Setter;

public class DataClientPool {
    private int index = 0;
    
    private List<DataClient> list = new ArrayList<>();

    private String ip;

    private Integer dataPort;

    private Channel channel;

    

    public DataClientPool(String ip, Integer dataPort, Channel channel) {
        this.ip = ip;
        this.dataPort = dataPort;
        this.channel = channel;
    }

    public synchronized DataClient getClient(String serviceKey, String address) throws Exception {
        int i = index;
        boolean f = false;
        DataClient dataClient = null;
        if (!list.isEmpty()) {
            for (;;) {
                if (i == index && f) {
                    break;
                } else if (i == index) {
                    f = true;
                }
                dataClient = list.get(index++);
                if (index >= list.size()) {
                    index = 0;
                }
                try {
                    ChannelPromise take = dataClient.take(serviceKey, address);
                    dataClient.setChannelPromise(take);
                    return dataClient;
                } catch (Exception e) {

                }
            }
        }
        dataClient = new DataClient(channel);
        dataClient.connect(ip, dataPort);
        ChannelPromise take = dataClient.take(serviceKey, address);
        dataClient.setChannelPromise(take);
        list.add(dataClient);
        return dataClient;
    }
}
