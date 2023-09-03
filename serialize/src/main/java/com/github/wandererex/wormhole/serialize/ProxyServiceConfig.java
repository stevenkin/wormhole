package com.github.wandererex.wormhole.serialize;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProxyServiceConfig {
    @Data
    public static class ServiceConfig {
        private String ip;
        private Integer port;
        private Integer mappingPort;
    }

    private Map<String, ServiceConfig> map = new HashMap<>();

    private String serverHost;

    private Integer serverPort;

    public  ServiceConfig getServiceConfig(String serviceKey) {
        return map.get(serviceKey);
    }

    public Map<String, ServiceConfig> getServiceConfigMap() {
        return new HashMap<>(map);
    }

    public void addConfig(String key, ServiceConfig config) {
        map.put(key, config);
    }
}
