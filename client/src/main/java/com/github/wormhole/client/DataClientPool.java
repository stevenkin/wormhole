package com.github.wormhole.client;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataClientPool {
    private Queue<DataClient> dataClientQueue = new LinkedBlockingQueue<>();

    public void init(String ip, Integer port) {
        for (int i = 0; i < 10; i++) {
            dataClientQueue.add(new DataClient(ip, port));
        }
    }

    public DataClient take() {
        return dataClientQueue.poll();
    }

    public void revert(DataClient dataClient) {
        dataClientQueue.add(dataClient);
    }
}
