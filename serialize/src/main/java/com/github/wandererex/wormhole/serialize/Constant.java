package com.github.wandererex.wormhole.serialize;

public interface Constant {
    String CLIENT_PUBLIC_KEY = "client_public_key";

    byte[] BUSINESS_MAGIC = {(byte) 0xc3, (byte) 0x11, (byte) 0xa3, (byte) 0x65};

    byte[] PING_MAGIC = {(byte) 0xc3, (byte) 0x15, (byte) 0xa7, (byte) 0x65};

    byte[] PONG_MAGIC = {(byte) 0xc3, (byte) 0x17, (byte) 0xab, (byte) 0x65};

    int opContinuation = 0;

    int opText = 1;

    int opBinary = 2;

    int opClose = 8;

    int opPing = 9;

    int opPong = 10;

    String SEND_SUCCESS = "SEND_SUCCESS";

    String SEND_FAILED = "send_failed";

}
