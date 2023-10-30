package com.github.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class FrameSerialization implements Serialization<Frame> {

    @Override
    public ByteBuf serialize(Frame msg, ByteBuf byteBuf) {
        int opCode = msg.getOpCode();
        byteBuf.writeInt(opCode);
        String sessionId = msg.getRequestId();
        if (StringUtils.isNotEmpty(sessionId)) {
            byte[] bytes = msg.getRequestId().getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        } else {
            byteBuf.writeInt(0);
        }
        String serviceKey = msg.getServiceKey();
        if (StringUtils.isNotEmpty(serviceKey)) {
            byte[] bytes = msg.getServiceKey().getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        } else {
            byteBuf.writeInt(0);
        }
        if (StringUtils.isNotEmpty(msg.getRealClientAddress())) {
            byte[] bytes = msg.getRealClientAddress().getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        } else {
            byteBuf.writeInt(0);
        }
        if (msg.getPayload() != null) {
            byteBuf.writeInt(msg.getPayload().readableBytes());
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
            String sessionId = new String(bytes, StandardCharsets.UTF_8);
            frame.setRequestId(sessionId);
        }
        n = byteBuf.readInt();
        if (n > 0) {
            bytes = new byte[n];
            byteBuf.readBytes(bytes, 0, n);
            String serviceKey = new String(bytes, StandardCharsets.UTF_8);
            frame.setServiceKey(serviceKey);
        }
        n = byteBuf.readInt();
        if (n > 0) {
            bytes = new byte[n];
            byteBuf.readBytes(bytes, 0, n);
            String realClientAddress = new String(bytes, StandardCharsets.UTF_8);
            frame.setRealClientAddress(realClientAddress);
        }
        n = byteBuf.readInt();
        if (n > 0) {
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(n);
            buffer.writeBytes(byteBuf, n);
            frame.setPayload(buffer);
        }
        return frame;
    }
}
