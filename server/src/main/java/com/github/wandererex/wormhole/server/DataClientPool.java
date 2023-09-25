package com.github.wandererex.wormhole.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataClientPool {
    private AtomicInteger index = new AtomicInteger();
    
    private List<DataClient> list = new ArrayList<>();

    public synchronized void addClient(DataClient client) {
        list.add(client);
    }

    public synchronized DataClient getClient() {
        int andIncrement = index.getAndIncrement();
        int i = andIncrement % list.size();
        DataClient dataClient = list.get(i);
        
        return dataClient;
    }
}
