package com.gypsyengineer.tlsbunny.tls13.connection.action.simple;

import com.gypsyengineer.tlsbunny.tls13.connection.action.AbstractAction;
import com.gypsyengineer.tlsbunny.tls13.connection.action.Action;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateVerify;

public class ProcessingCertificateVerify extends AbstractAction {

    @Override
    public String name() {
        return "processing a CertificateVerify";
    }

    @Override
    public Action run() {
        CertificateVerify certificateVerify = context.factory.parser().parseCertificateVerify(in);
        output.info("received a CertificateVerify message");

        return this;
    }

}
