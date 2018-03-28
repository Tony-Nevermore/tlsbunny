package com.gypsyengineer.tlsbunny.tls13.connection;

import com.gypsyengineer.tlsbunny.tls13.crypto.AEAD;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSInnerPlaintext;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSPlaintext;

import java.io.IOException;

public class IncomingApplicationData extends AbstractAction {

    @Override
    public String name() {
        return "application data";
    }

    @Override
    public Action run() throws Exception {
        TLSPlaintext tlsCiphertext = factory.parser().parseTLSPlaintext(buffer);

        if (!tlsCiphertext.containsApplicationData()) {
            throw new IOException("TLSCiphertext should contains application data");
        }

        TLSInnerPlaintext tlsInnerPlaintext = decrypt(tlsCiphertext);
        if (!tlsInnerPlaintext.containsApplicationData()) {
            throw new IOException("TLSInnerPlaintext should contain application data");
        }

        byte[] data = tlsInnerPlaintext.getContent();
        output.info("received data (%d bytes)%n%s", data.length, new String(data));

        return this;
    }

    private TLSInnerPlaintext decrypt(TLSPlaintext tlsCiphertext) throws Exception {
        return decrypt(tlsCiphertext.getFragment(), AEAD.getAdditionalData(tlsCiphertext));
    }

    private TLSInnerPlaintext decrypt(byte[] ciphertext, byte[] additional_data)
            throws Exception {

        context.applicationDataDecryptor.start();
        context.applicationDataDecryptor.updateAAD(additional_data);
        context.applicationDataDecryptor.update(ciphertext);
        byte[] plaintext = context.applicationDataDecryptor.finish();

        return factory.parser().parseTLSInnerPlaintext(plaintext);
    }
}
