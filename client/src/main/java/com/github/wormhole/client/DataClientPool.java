package com.github.wormhole.client;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataClientPool {
    private String ip;

    private Integer port;

    public DataClientPool(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }
    private Queue<DataClient> dataClientQueue = new LinkedBlockingQueue<>();

    public void init() {
        for (int i = 0; i < 10; i++) {
            dataClientQueue.add(new DataClient(ip, port));
        }
    }

    public DataClient take() {
        DataClient client = dataClientQueue.poll();
        if (client == null) {
            for (;;) {
                DataClient dataClient = new DataClient(ip, port);
                try {
                    dataClient.connect();
                    client = dataClientQueue.poll();
                    if (client != null) {
                        return client;
                    }
                } catch (Exception e) {
                }
            }
        }
        return client;
    }

    public void revert(DataClient dataClient) {
        dataClientQueue.add(dataClient);
    }
}
