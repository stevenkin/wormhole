package com.github.wormhole.serialize;

import io.netty.handler.codec.LengthFieldPrepender;

public class FrameEncoder extends LengthFieldPrepender {
    public FrameEncoder() {
        super(2);
    }
}
