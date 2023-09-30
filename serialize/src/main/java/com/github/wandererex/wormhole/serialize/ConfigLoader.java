package com.github.wandererex.wormhole.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.wandererex.wormhole.serialize.ProxyServiceConfig.ServiceConfig;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigLoader {
    public static ProxyServiceConfig load(String path) throws IOException {
        InputStream inputStream = ConfigLoader.class.getResourceAsStream(path);
        byte[] bytes = IOUtils.toByteArray(inputStream);
        String s = new String(bytes, StandardCharsets.UTF_8);
        ProxyServiceConfig parse = parse(s);
        return parse;
    }

    public static ProxyServiceConfig parse(String data) throws IOException {
        String s = data;
        JSONObject jsonObject = JSON.parseObject(s);
        ProxyServiceConfig proxyServiceConfig = new ProxyServiceConfig();
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            if (entry.getValue() instanceof  JSONObject) {
                proxyServiceConfig.addConfig(entry.getKey(), ((JSONObject) entry.getValue()).toJavaObject(ProxyServiceConfig.ServiceConfig.class));
            }
        }
        proxyServiceConfig.setServerHost(jsonObject.getString("serverHost"));
        proxyServiceConfig.setServerPort(jsonObject.getInteger("serverPort"));
        return proxyServiceConfig;
    }

    public static String serialize(ProxyServiceConfig config) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverHost", config.getServerHost());
        jsonObject.put("serverPort", config.getServerPort());
        Map<String, ServiceConfig> map = config.getMap();
        map.forEach((k, v) -> jsonObject.put(k, JSONObject.toJSON(v)));
        return jsonObject.toJSONString();
    }

}
