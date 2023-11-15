package com.github.wormhole.client;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Map.Entry;
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

    private Map<String, String> dataClientAssignedPeerMap = new ConcurrentHashMap<>();

    private Context context;

    private String serviceKey;

    public DataClientPool(String ip, Integer port, int connType, Context context) {
        this.ip = ip;
        this.port = port;
        this.connType = connType;
        this.context = context;
    }
    private Queue<DataClient> dataClientQueue = new LinkedBlockingQueue<>();

    public void init() {
        for (int i = 0; i < 10; i++) {
            DataClient dataClient = new DataClient(ip, port, connType, context, this);
            try {
                dataClient.connect();
                dataClientQueue.add(dataClient);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public DataClient take() {
        DataClient client = dataClientQueue.poll();
        if (client == null) {
            for (;;) {
                DataClient dataClient = new DataClient(ip, port, connType, context, this);
                try {
                    dataClient.connect();
                    client = dataClient;
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
        assignedDataClients.remove(dataClient.getId());
        Optional<Entry<String, String>> findFirst = dataClientAssignedPeerMap.entrySet().stream().filter(e -> e.getValue().equals(dataClient.getId())).findFirst();
        findFirst.ifPresent(e -> dataClientAssignedPeerMap.remove(e.getKey()));
    }

    public DataClient getAssignedDataClient(String id) {
        return assignedDataClients.get(id);
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public int getConnType() {
        return connType;
    }

    public Map<String, DataClient> getAssignedDataClients() {
        return assignedDataClients;
    }

    public Context getContext() {
        return context;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Queue<DataClient> getDataClientQueue() {
        return dataClientQueue;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public void setClientAssignedPeer(String peerAddress, String id) {
        this.dataClientAssignedPeerMap.put(peerAddress, id);
    }

    public Map<String, String> getDataClientAssignedPeerMap() {
        return dataClientAssignedPeerMap;
    }
    
}
