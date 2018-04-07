package com.gypsyengineer.tlsbunny.tls13.test.client;

import com.gypsyengineer.tlsbunny.tls13.connection.*;
import com.gypsyengineer.tlsbunny.tls13.fuzzer.MutatedStructFactory;
import com.gypsyengineer.tlsbunny.tls13.fuzzer.Target;
import com.gypsyengineer.tlsbunny.tls13.struct.StructFactory;
import com.gypsyengineer.tlsbunny.utils.Output;

import java.io.IOException;

public class FuzzyCertificate implements Runnable {

    public static final String HTTP_GET_REQUEST = "GET / HTTP/1.1\n\n";

    private final Output output;
    private final FuzzerConfig config;
    private final MutatedStructFactory fuzzer;

    FuzzyCertificate(Output output, FuzzerConfig config) {
        fuzzer = new MutatedStructFactory(
                StructFactory.getDefault(),
                output,
                config.minRatio(),
                config.maxRatio()
        );
        fuzzer.setTarget(Target.certificate);
        fuzzer.setMode(config.mode());
        fuzzer.setStartTest(config.startTest());
        fuzzer.setEndTest(config.endTest());

        this.output = output;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            output.prefix(Thread.currentThread().getName());
            while (fuzzer.canFuzz()) {
                output.info("test %d", fuzzer.getTest());
                output.info("now fuzzer's state is '%s'", fuzzer.getState());
                try {
                    Engine.init()
                            .target(config.host())
                            .target(config.port())
                            .set(fuzzer)
                            .set(output)
                            .send(new OutgoingClientHello())
                            .send(new OutgoingChangeCipherSpec())
                            .expect(new IncomingServerHello())
                            .expect(new IncomingChangeCipherSpec())
                            .expect(new IncomingEncryptedExtensions())
                            .expect(new IncomingCertificateRequest())
                            .expect(new IncomingCertificate())
                            .expect(new IncomingCertificateVerify())
                            .expect(new IncomingFinished())
                            .send(new OutgoingCertificate()
                                    .certificate(config.clientCertificate()))
                            .send(new OutgoingCertificateVerify()
                                    .key(config.clientKey()))
                            .send(new OutgoingFinished())
                            .allow(new IncomingNewSessionTicket())
                            .send(new OutgoingApplicationData(HTTP_GET_REQUEST))
                            .expect(new IncomingApplicationData())
                            .connect();
                } finally {
                    output.flush();
                    fuzzer.moveOn();
                }
            }
        } catch (IOException e) {
            output.info("looks like the server closed connection", e);
        } catch (Exception e) {
            output.achtung("what the hell? unexpected exception", e);
        } finally {
            output.flush();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FuzzerConfig config = new FuzzerConfig();

        int threads = config.threads();
        if (threads > 1) {
            new MultipleThreads().add(config).submit();
        } else {
            config.create().run();
        }
    }

    public static class FuzzerConfig extends CommonConfig {

        @Override
        public FuzzyCertificate create() {
            return new FuzzyCertificate(new Output(), this);
        }

        @Override
        public CommonConfig copy() {
            FuzzerConfig clone = new FuzzerConfig();
            clone.host = host;
            clone.port = port;
            clone.minRatio = minRatio;
            clone.maxRatio = maxRatio;
            clone.threads = threads;
            clone.parts = parts;
            clone.startTest = startTest;
            clone.endTest = endTest;
            clone.target = target;
            clone.mode = mode;

            return clone;
        }
    }

}
