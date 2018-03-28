package com.gypsyengineer.tlsbunny.tls13.connection;

import com.gypsyengineer.tlsbunny.tls13.struct.*;

import java.io.IOException;

public class IncomingAlert extends AbstractAction {

    @Override
    public String name() {
        return "Alert";
    }

    @Override
    public Action run() throws Exception {
        TLSPlaintext tlsPlaintext = factory.parser().parseTLSPlaintext(buffer);

        Alert alert;
        if (tlsPlaintext.containsAlert()) {
            alert = factory.parser().parseAlert(tlsPlaintext.getFragment());
        } else if (tlsPlaintext.containsApplicationData()) {
            TLSInnerPlaintext tlsInnerPlaintext = factory.parser().parseTLSInnerPlaintext(
                    context.applicationDataDecryptor.decrypt(tlsPlaintext));

            if (!tlsInnerPlaintext.containsAlert()) {
                throw new IOException("expected an alert");
            }

            alert = factory.parser().parseAlert(tlsInnerPlaintext.getContent());
        } else {
            throw new IOException("expected an alert");
        }

        output.info("received an alert: %s", alert);

        return this;
    }

    
}