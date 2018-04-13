package com.gypsyengineer.tlsbunny.tls13.connection.action.composite;

import com.gypsyengineer.tlsbunny.tls13.connection.action.AbstractAction;
import com.gypsyengineer.tlsbunny.tls13.connection.action.Action;
import com.gypsyengineer.tlsbunny.tls13.struct.Handshake;

import java.io.IOException;

public class IncomingCertificate extends AbstractAction {

    @Override
    public String name() {
        return "Certificate";
    }

    @Override
    public Action run() throws Exception {
        Handshake handshake = processEncryptedHandshake();
        if (!handshake.containsCertificate()) {
            throw new IOException("expected a Certificate message");
        }

        processCertificate(handshake);

        return this;
    }

    private void processCertificate(Handshake handshake) {
        factory.parser().parseCertificate(
                handshake.getBody(),
                buf -> factory.parser().parseX509CertificateEntry(buf));
        context.setServerCertificate(handshake);
    }
}
