package com.github.wormhole.proxy.processor;

import java.util.Map;

import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.Processor;
import com.github.wormhole.common.config.ProxyServiceConfig.ServiceConfig;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelHandlerContext;

public class DataChannelProcessor implements Processor{
    private Proxy proxy;

    private Map<String, DataClientPool> serviceClientPool;

    public DataChannelProcessor(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x2;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        String serviceKey = msg.getServiceKey();
        String realClientAddress = msg.getRealClientAddress();
        DataClient dataClient = proxy.getDataClientPool().take();
        DataClientPool dataClientPool = serviceClientPool.get(serviceKey);
        ServiceConfig serviceConfig = proxy.getConfig().getMap().get(serviceKey);
        if (dataClient == null) {
            serviceClientPool.put(serviceKey, new DataClientPool(serviceConfig.getIp(), serviceConfig.getPort()));
            dataClientPool = serviceClientPool.get(serviceKey);
        }
        DataClient serviceClient = dataClientPool.take();
    }
    
}
