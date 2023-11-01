package com.github.wormhole.client;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DataClientPool {
    private String ip;

    private Integer port;

    /**
     * 连接类型（1.连接到代理服务器， 2.连接到内网服务）
     */
    private int connType;

    private Map<String, DataClient> assignedDataClients = new ConcurrentHashMap<>();

    public DataClientPool(String ip, Integer port, int connType) {
        this.ip = ip;
        this.port = port;
        this.connType = connType;
    }
    private Queue<DataClient> dataClientQueue = new LinkedBlockingQueue<>();

    public void init() {
        for (int i = 0; i < 10; i++) {
            dataClientQueue.add(new DataClient(ip, port, connType));
        }
    }

    public DataClient take() {
        DataClient client = dataClientQueue.poll();
        if (client == null) {
            for (;;) {
                DataClient dataClient = new DataClient(ip, port, connType);
                try {
                    dataClient.connect();
                    dataClientQueue.add(dataClient);
                    client = dataClientQueue.poll();
                    if (client != null) {
                        assignedDataClients.put(client.getId(), client);
                        return client;
                    }
                } catch (Exception e) {
                }
            }
        }
        if (client != null) {
            assignedDataClients.put(client.getId(), client);
        }
        return client;
    }

    public void revert(DataClient dataClient) {
        dataClientQueue.add(dataClient);
    }

    public DataClient getAssignedDataClient(String id) {
        return assignedDataClients.get(id);
    }
}
