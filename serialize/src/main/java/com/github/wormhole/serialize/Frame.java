package com.github.wormhole.serialize;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Frame {
    /**
     * 0x1 代理发起注册
     * 0x10 代理注册成功
     * -0x1 统一失败码
     * 0x2 创建数据传输通道
     */
    private int opCode;

    private String requestId;

    private String proxyId;

    private String serviceKey;

    private String realClientAddress;

    private ByteBuf payload;
}
