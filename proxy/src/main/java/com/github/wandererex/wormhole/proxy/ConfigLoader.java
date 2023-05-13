package com.github.wandererex.wormhole.proxy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigLoader {
    public static ProxyServiceConfig load(String path) throws IOException {
        File file = new File(path);
        InputStream inputStream = new FileInputStream(file);
        byte[] bytes = IOUtils.toByteArray(inputStream);
        String s = new String(bytes, StandardCharsets.UTF_8);
        JSONObject jsonObject = JSON.parseObject(s);
        ProxyServiceConfig proxyServiceConfig = new ProxyServiceConfig();
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            proxyServiceConfig.addConfig(entry.getKey(), ((JSONObject)entry.getValue()).toJavaObject(ProxyServiceConfig.ServiceConfig.class));
        }
        return proxyServiceConfig;
    }
}
