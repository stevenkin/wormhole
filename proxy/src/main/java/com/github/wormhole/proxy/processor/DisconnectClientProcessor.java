package com.github.wormhole.proxy.processor;

import java.nio.charset.Charset;

import com.github.wormhole.client.DataClient;
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
        String string = payload.readCharSequence(payload.readableBytes(), Charset.forName("UTF-8")).toString();
        DataClient assignedDataClient = proxy.getDataClientPool().getAssignedDataClient(string);
        if (assignedDataClient != null) {
            ((SocketChannel)(assignedDataClient.getDirectClient().getChannel())).shutdownOutput();
            assignedDataClient.revert();;
        }
    }
    
}
