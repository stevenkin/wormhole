package com.github.wandererex.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.lang3.StringUtils;


import java.nio.charset.StandardCharsets;

public class FrameSerialization implements Serialization<Frame> {

    @Override
    public ByteBuf serialize(Frame msg, ByteBuf byteBuf) {
        int opCode = msg.getOpCode();
        byteBuf.writeInt(opCode);
        String serviceKey = msg.getServiceKey();
        if (StringUtils.isNotEmpty(serviceKey)) {
            byte[] bytes = msg.getServiceKey().getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
        if (msg.getPayload() != null) {
            byteBuf.writeInt(msg.getPayload().length);
            byteBuf.writeBytes(msg.getPayload());
        } else {
            byteBuf.writeInt(0);
        }
        return byteBuf;
    }

    @Override
    public Frame deserialize(ByteBuf byteBuf) {
        Frame frame = new Frame();
        frame.setOpCode(byteBuf.readInt());
        int n = byteBuf.readInt();
        byte[] bytes = null;
        if (n > 0) {
            bytes = new byte[n];
            byteBuf.readBytes(bytes, 0, n);
            String serviceKey = new String(bytes, StandardCharsets.UTF_8);
            frame.setServiceKey(serviceKey);
        }
        if (n > 0) {
            bytes = new byte[n];
            byteBuf.readBytes(bytes, 0, n);
            frame.setPayload(bytes);
        }
        return frame;
    }
}
