package com.github.wormhole.proxy.processor;

import com.github.wormhole.client.Processor;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyRegisterAckProcessor implements Processor{
    private Proxy proxy;

    public ProxyRegisterAckProcessor(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x10;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        log.info("代理注册成功{}", msg);
        proxy.setProxyId(msg.getProxyId());
    }
    
}
