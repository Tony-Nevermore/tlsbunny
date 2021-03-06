package com.gypsyengineer.tlsbunny.tls;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Random implements Struct {

    public static final int LENGTH = 32;

    private byte[] bytes;

    public Random(byte[] bytes) {
        if (bytes.length != LENGTH) {
            throw new IllegalArgumentException();
        }
        
        this.bytes = bytes.clone();
    }

    public Random() {
        this(new byte[LENGTH]);
    }

    public byte[] getBytes() {
        return bytes.clone();
    }

    public void setBytes(byte[] bytes) {
        if (bytes.length != LENGTH) {
            throw new IllegalArgumentException();
        }
        
        this.bytes = bytes.clone();
    }

    @Override
    public int encodingLength() {
        return LENGTH;
    }

    @Override
    public byte[] encoding() throws IOException {
        return ByteBuffer.allocate(LENGTH).put(bytes).array();
    }

    public static Random parse(ByteBuffer buffer) {
        byte[] bytes = new byte[LENGTH];
        buffer.get(bytes);
        return new Random(bytes);
    }

}
