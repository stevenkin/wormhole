package com.github.wandererex.wormhole.proxy;

import com.github.wandererex.wormhole.serialize.Frame;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CountDownLatch;

public class Proxy {
    public static CountDownLatch latch = new CountDownLatch(1);
    private String serverHost;

    private Integer serverPort;

    private ProxyServiceConfig config;

    private Channel channel;

    private ProxyClient client;

    public Proxy(String serverHost, Integer serverPort, String configPath) throws Exception {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.config = ConfigLoader.load(configPath);
        client = new ProxyClient(config);
    }

    public void start() throws Exception {
        channel = client.connect(serverHost, serverPort);
        online(channel);
        client.syncAuth();
        client.checkIdle();
        latch.await();
    }

    private void online(Channel channel) {
        Frame frame = new Frame(0x1, null, null);
        channel.writeAndFlush(frame);
    }

    public static void main(String[] args) throws Exception {
        String serverHost = null;
        String serverPort = null;
        String configPath = null;
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
               if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--configPath")) {
                   if (i + 1 < args.length) {
                       String arg = args[i + 1];
                       if (StringUtils.isNotEmpty(arg)) {
                           configPath = arg;
                       }
                   }
               }
               if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--serverHost")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            serverHost = arg;
                        }
                    }
               }
               if (StringUtils.isNotEmpty(args[i]) && args[i].equals("--serverPort")) {
                    if (i + 1 < args.length) {
                        String arg = args[i + 1];
                        if (StringUtils.isNotEmpty(arg)) {
                            serverPort = arg;
                        }
                    }
               }
            }
            if (serverHost != null && serverPort != null && configPath != null) {
                Proxy proxy = new Proxy(serverHost, Integer.parseInt(serverPort), configPath);
                proxy.start();
            }
        }
    }
}
