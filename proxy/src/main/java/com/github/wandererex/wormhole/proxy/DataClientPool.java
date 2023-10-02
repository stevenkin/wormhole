package com.github.wandererex.wormhole.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;

public class DataClientPool {
    private int index = 0;
    
    private List<DataClient> list = new ArrayList<>();

    private String ip;

    private Integer dataPort;

    

    public DataClientPool(String ip, Integer dataPort) {
        this.ip = ip;
        this.dataPort = dataPort;
    }

    public synchronized DataClient getClient() throws Exception {
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
                boolean take = dataClient.take();
                if (take) {
                    return dataClient;
                }
            }
        }
        dataClient = new DataClient();
        dataClient.connect(ip, dataPort);
        dataClient.take();
        list.add(dataClient);
        return dataClient;
    }
}
