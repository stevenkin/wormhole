package com.github.wormhole.server.processor;

import com.github.wormhole.client.Processor;
import com.github.wormhole.serialize.Frame;
import com.github.wormhole.server.ProxyServer;
import com.github.wormhole.server.Server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisconnectClientPocessor implements Processor{
    private Server server;

    public DisconnectClientPocessor(Server server) {
        this.server = server;
    }

    @Override
    public boolean isSupport(Frame frame) {
        return frame.getOpCode() == 0x4;
    }

    @Override
    public void process(ChannelHandlerContext ctx, Frame msg) throws Exception {
        log.info("关闭客户端链接{}", msg);
        ProxyServer proxyServer = server.getProxyServer(msg.getProxyId());
        if (proxyServer != null) {
            Channel channel = proxyServer.getClientHandler().getClientChannelMap().get(msg.getRealClientAddress());
            if (channel != null && channel.isActive()) {
                ((SocketChannel)channel).shutdownOutput();
                msg.setOpCode(0x40);
                Channel channel2 = server.getProxyIdChannelMap().get(msg.getProxyId());
                if(channel2 != null) {
                    channel2.writeAndFlush(msg);
                }
            }
        }
    }
    
}
