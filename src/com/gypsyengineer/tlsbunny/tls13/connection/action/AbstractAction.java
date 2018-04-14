package com.gypsyengineer.tlsbunny.tls13.connection.action;

import com.gypsyengineer.tlsbunny.tls.Random;
import com.gypsyengineer.tlsbunny.tls13.crypto.AEAD;
import com.gypsyengineer.tlsbunny.tls13.crypto.HKDF;
import com.gypsyengineer.tlsbunny.tls13.handshake.Context;
import com.gypsyengineer.tlsbunny.tls13.handshake.Negotiator;
import com.gypsyengineer.tlsbunny.tls13.struct.*;
import com.gypsyengineer.tlsbunny.utils.Output;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class AbstractAction implements Action {

    // TODO: it should be moved to a common class
    public static final long DEFAULT_SEED = 0;
    public static final long SEED = Long.getLong("tlsbunny.seed", DEFAULT_SEED);

    protected StructFactory factory;
    protected ByteBuffer in;
    protected ByteBuffer out;
    protected Output output;
    protected SignatureScheme scheme;
    protected NamedGroup group;
    protected CipherSuite suite;
    protected Negotiator negotiator;
    protected HKDF hkdf;
    protected Context context;

    @Override
    public String name() {
        return "unknown action";
    }

    @Override
    public Action set(Output output) {
        this.output = output;
        return this;
    }

    @Override
    public Action set(StructFactory factory) {
        this.factory = factory;
        return this;
    }

    @Override
    public Action set(SignatureScheme scheme) {
        this.scheme = scheme;
        return this;
    }

    @Override
    public Action set(NamedGroup group) {
        this.group = group;
        return this;
    }

    @Override
    public Action set(CipherSuite suite) {
        this.suite = suite;
        return this;
    }

    @Override
    public Action set(Negotiator negotiator) {
        this.negotiator = negotiator;
        return this;
    }

    @Override
    public Action set(HKDF hkdf) {
        this.hkdf = hkdf;
        return this;
    }

    @Override
    public Action set(Context context) {
        this.context = context;
        return this;
    }

    @Override
    public Action set(ByteBuffer buffer) {
        this.in = buffer;
        return this;
    }

    @Override
    public boolean produced() {
        return out != null && out.remaining() > 0;
    }

    @Override
    public ByteBuffer data() {
        return out;
    }

    // helper methods

    protected void updateContext(Handshake handshake) {
        if (handshake.containsClientHello()) {
            if (!context.hasFirstClientHello()) {
                context.setFirstClientHello(handshake);
            } else if (!context.hasSecondClientHello()) {
                context.setSecondClientHello(handshake);
            } else {
                throw new IllegalArgumentException(
                        "two ClientHello messages have been already set");
            }
        }

        if (handshake.containsServerHello()) {
            context.setServerHello(handshake);
        }

        if (handshake.containsEncryptedExtensions()) {
            context.setEncryptedExtensions(handshake);
        }

        // TODO: we need to be able to set both server and client
        //       Certificates and CertificateVerify messages

        // TODO: set the test of messages
    }

    protected byte[] processEncrypted(AEAD decryptor, ContentType expectedType) throws Exception {
        TLSPlaintext tlsPlaintext = factory.parser().parseTLSPlaintext(in);
        if (tlsPlaintext.containsAlert()) {
            Alert alert = factory.parser().parseAlert(tlsPlaintext.getFragment());
            context.setAlert(alert);
            throw new IOException(String.format("received an alert: %s", alert));
        }

        if (!tlsPlaintext.containsApplicationData()) {
            throw new IOException("expected a TLSCiphertext");
        }

        TLSInnerPlaintext tlsInnerPlaintext = factory.parser().parseTLSInnerPlaintext(
                decryptor.decrypt(tlsPlaintext));

        if (!expectedType.isAlert() && tlsInnerPlaintext.containsAlert()) {
            Alert alert = factory.parser().parseAlert(tlsInnerPlaintext.getContent());
            context.setAlert(alert);
            throw new IOException(String.format("received an alert: %s", alert));
        }

        if (!expectedType.equals(tlsInnerPlaintext.getType())) {
            throw new IOException(
                    String.format("expected %, but received %s",
                            expectedType, tlsInnerPlaintext.getType()));
        }

        return tlsInnerPlaintext.getContent();
    }

    protected Handshake processEncryptedHandshake() throws Exception {
        return factory.parser().parseHandshake(
                processEncrypted(context.handshakeDecryptor, ContentType.handshake));
    }

    protected Handshake toHandshake(HandshakeMessage message) throws IOException {
        return factory.createHandshake(message.type(), message.encoding());
    }

    protected Extension wrap(SupportedVersions supportedVersions) throws IOException {
        return factory.createExtension(
                ExtensionType.supported_versions, supportedVersions.encoding());
    }

    protected Extension wrap(SignatureSchemeList signatureSchemeList) throws IOException {
        return factory.createExtension(
                ExtensionType.signature_algorithms, signatureSchemeList.encoding());
    }

    protected Extension wrap(NamedGroupList namedGroupList) throws IOException {
        return factory.createExtension(
                ExtensionType.supported_groups, namedGroupList.encoding());
    }

    protected Extension wrap(KeyShare keyShare) throws IOException {
        return factory.createExtension(
                ExtensionType.key_share, keyShare.encoding());
    }

    public static Random createRandom() {
        java.util.Random generator = new java.util.Random(SEED);
        byte[] random_bytes = new byte[Random.LENGTH];
        generator.nextBytes(random_bytes);
        Random random = new Random();
        random.setBytes(random_bytes);

        return random;
    }
}
