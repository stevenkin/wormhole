package com.github.wandererex.wormhole.proxy;

import com.alibaba.fastjson.JSON;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigLoader {
    public static ProxyServiceConfig load(String path) throws IOException {
        File file = new File(path);
        InputStream inputStream = new FileInputStream(file);
        ProxyServiceConfig config = JSON.parseObject(inputStream, ProxyServiceConfig.class);
        return config;
    }
}
