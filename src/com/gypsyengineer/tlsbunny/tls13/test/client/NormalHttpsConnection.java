package com.gypsyengineer.tlsbunny.tls13.test.client;

import com.gypsyengineer.tlsbunny.tls13.connection.*;

public class NormalHttpsConnection {

    public static final String HTTP_GET_REQUEST = "GET / HTTP/1.1\n\n";

    public static void main(String[] args) throws Exception {
        Engine.init()
                .target("localhost")
                .target(10101)
                .send(new OutgoingClientHello())
                .send(new OutgoingChangeCipherSpec())
                .expect(new IncomingServerHello())
                .expect(new IncomingChangeCipherSpec())
                .expect(new IncomingEncryptedExtensions())
                .expect(new IncomingCertificate())
                .expect(new IncomingCertificateVerify())
                .expect(new IncomingFinished())
                .send(new OutgoingFinished())
                .allow(new IncomingNewSessionTicket())
                .send(new OutgoingApplicationData(HTTP_GET_REQUEST))
                .expect(new IncomingApplicationData())
                .connect()
                .check(new Success());
    }

}
