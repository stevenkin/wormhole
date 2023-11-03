package com.github.wormhole.proxy.processor;

import java.nio.charset.Charset;
import java.util.Map;

import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.Processor;
import com.github.wormhole.common.config.ProxyServiceConfig.ServiceConfig;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
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
        DataClient dataClient = proxy.getDataClientPool().take();
        dataClient.setPeerClientAddress(msg.getRealClientAddress());
        DataClientPool dataClientPool = serviceClientPool.get(serviceKey);
        ServiceConfig serviceConfig = proxy.getConfig().getMap().get(serviceKey);
        if (dataClientPool == null) {
            serviceClientPool.put(serviceKey, new DataClientPool(serviceConfig.getIp(), serviceConfig.getPort(), 2, proxy));
            dataClientPool = serviceClientPool.get(serviceKey);
            dataClientPool.setServiceKey(serviceKey);
        }
        DataClient serviceClient = dataClientPool.take();
        serviceClient.setPeerClientAddress(msg.getRealClientAddress());
        serviceClient.refresh(dataClient);
        dataClient.refresh(serviceClient);
        msg.setOpCode(0x20);
        msg.setProxyId(proxy.getProxyId());
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        buffer.writeCharSequence(dataClient.getChannel().id().toString(), Charset.forName("UTF-8"));
        msg.setPayload(buffer);
        ctx.writeAndFlush(msg);
    }

    
    
}
