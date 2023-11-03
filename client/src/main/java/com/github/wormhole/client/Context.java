package com.github.wormhole.client;

public interface Context {
    void write(Object msg);
    
    Object read();

    String id();
    
}
