package com.github.wormhole.proxy.processor;

import java.nio.charset.Charset;

import com.github.wormhole.client.DataClient;
import com.github.wormhole.client.DataClientPool;
import com.github.wormhole.client.Processor;
import com.github.wormhole.proxy.Proxy;
import com.github.wormhole.serialize.Frame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;

public class DisconnectClientProcessor implements Processor{
    private Proxy proxy;

    public DisconnectClientProcessor(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x4;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        ByteBuf payload = msg.getPayload();
        String realClientAddress = msg.getRealClientAddress();
        String serviceKey = msg.getServiceKey();
        DataClientPool dataClientPool = proxy.getDataChannelProcessor().getServiceClientPool().get(serviceKey);
        if (dataClientPool != null) {
            String string = dataClientPool.getDataClientAssignedPeerMap().get(realClientAddress);
            if (string != null) {
                DataClient assignedDataClient = dataClientPool.getAssignedDataClient(string);
                if (assignedDataClient != null) {
                    ((SocketChannel)(assignedDataClient.getChannel())).shutdownOutput();
                }
            }
        }
    }
    
}
