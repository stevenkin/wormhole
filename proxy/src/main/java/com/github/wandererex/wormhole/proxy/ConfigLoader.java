package com.github.wandererex.wormhole.proxy;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.InputStream;

public class ConfigLoader {
    public static ProxyServiceConfig load(String path) throws IOException {
        InputStream resourceAsStream = ConfigLoader.class.getClass().getResourceAsStream(path);
        ProxyServiceConfig config = JSON.<ProxyServiceConfig>parseObject(resourceAsStream, ProxyServiceConfig.class);
        return config;
    }
}
