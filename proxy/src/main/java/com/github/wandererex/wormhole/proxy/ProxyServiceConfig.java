package com.github.wandererex.wormhole.proxy;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

public class ProxyServiceConfig {
    @Data
    static class ServiceConfig {
        private String ip;
        private Integer port;
    }

    private Map<String, ServiceConfig> map = new HashMap<>();

    public  ServiceConfig getServiceConfig(String serviceKey) {
        return map.get(serviceKey);
    }
}
