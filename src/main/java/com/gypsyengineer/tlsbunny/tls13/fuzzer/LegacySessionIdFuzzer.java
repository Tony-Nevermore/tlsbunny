package com.gypsyengineer.tlsbunny.tls13.fuzzer;

import com.gypsyengineer.tlsbunny.tls.Vector;
import com.gypsyengineer.tlsbunny.utils.Output;

import java.io.IOException;

public class LegacySessionIdFuzzer implements Fuzzer<Vector<Byte>> {

    public static final int LENGTH_BYTES = 1;

    public static class FuzzedLegacySessionId extends FuzzedVector<Byte> {

        public FuzzedLegacySessionId(int encodingLength, byte[] bytes) {
            super(LENGTH_BYTES, encodingLength, bytes);
        }
    }

    private static final Generator[] generators = {
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, sessionId.bytes()),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    1, sessionId.bytes()),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, sessionId.bytes()),

            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(1, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(100, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(255, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(1, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(100, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(255, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(1, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(100, 0x00)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(255, 0x00)),

            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(1, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(100, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(255, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(1, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(100, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(255, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(1, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(100, 0x17)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(255, 0x17)),

            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(1, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(100, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(255, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(1, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(100, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(255, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(1, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(100, 0xFF)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(255, 0xFF)),

            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(1)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(100)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    0, generateArray(255)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(1)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(100)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    100, generateArray(255)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(1)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(100)),
            (sessionId, output) -> new FuzzedLegacySessionId(
                    255, generateArray(255)),
    };

    private int state = 0;
    private int end = generators.length - 1;

    private Output output;

    @Override
    public String getState() {
        return Integer.toString(state);
    }

    @Override
    public void setState(String state) {
        setStartTest(Integer.parseInt(state));
    }

    @Override
    public void setStartTest(long state) {
        this.state = check(state);
    }

    @Override
    public void setEndTest(long end) {
        this.end = check(end);
    }

    @Override
    public void setOutput(Output output) {
        this.output = output;
    }

    @Override
    public Output getOutput() {
        return output;
    }

    @Override
    public long getTest() {
        return state;
    }

    @Override
    public boolean canFuzz() {
        return state <= end;
    }

    @Override
    public void moveOn() {
        if (state == Long.MAX_VALUE) {
            throw new IllegalStateException();
        }
        state++;
    }

    @Override
    public final Vector<Byte> fuzz(Vector<Byte> legacySessionId) {
        try {
            return generators[state].run(legacySessionId, output);
        } catch (IOException e) {
            // TODO: can we do better?
            throw new RuntimeException(e);
        }
    }

    private static int check(long state) {
        if (state < 0 || state > generators.length - 1) {
            throw new IllegalArgumentException(
                    String.format("state should be in [0, %d], but %d received",
                            generators.length - 1, state));
        }

        return (int) state;
    }

    private static byte[] generateArray(int length) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = (byte) (0xFF & i);
        }
        return array;
    }

    private static byte[] generateArray(int length, int value) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = (byte) (0xFF & value);
        }
        return array;
    }

    private interface Generator {
        Vector<Byte> run (Vector<Byte> sessionId, Output output) throws IOException;
    }
}
