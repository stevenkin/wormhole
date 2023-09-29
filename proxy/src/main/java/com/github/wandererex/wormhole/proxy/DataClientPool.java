package com.github.wandererex.wormhole.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataClientPool {
    private int index = 0;
    
    private List<DataClient> list = new ArrayList<>();

    private Integer dataPort;

    

    public DataClientPool(Integer dataPort) {
        this.dataPort = dataPort;
    }

    public synchronized void addClient(DataClient client) {
        list.add(client);
    }

    public synchronized DataClient getClient() {
        int i = index;
        boolean f = false;
        DataClient dataClient = null;
        for (;;) {
            if (i == index && f) {
                break;
            } else if (i == index) {
                f = true;
            }
            dataClient = list.get(index++);
            boolean take = dataClient.take();
            if (take) {
                return dataClient;
            }
            if (index >= list.size()) {
                index = 0;
            }
        }
        dataClient = new DataClient();
        dataClient.connect(dataPort, i)
        return dataClient;
    }
}
