package com.gypsyengineer.tlsbunny.tls13.fuzzer;

import com.gypsyengineer.tlsbunny.tls.Random;
import com.gypsyengineer.tlsbunny.tls.UInt16;
import com.gypsyengineer.tlsbunny.tls.UInt24;
import com.gypsyengineer.tlsbunny.tls.Vector;
import com.gypsyengineer.tlsbunny.tls13.struct.CipherSuite;
import com.gypsyengineer.tlsbunny.tls13.struct.ClientHello;
import com.gypsyengineer.tlsbunny.tls13.struct.CompressionMethod;
import com.gypsyengineer.tlsbunny.tls13.struct.ContentType;
import com.gypsyengineer.tlsbunny.tls13.struct.Extension;
import com.gypsyengineer.tlsbunny.tls13.struct.ExtensionType;
import com.gypsyengineer.tlsbunny.tls13.struct.Handshake;
import com.gypsyengineer.tlsbunny.tls13.struct.HandshakeType;
import com.gypsyengineer.tlsbunny.tls13.struct.ProtocolVersion;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSPlaintext;
import java.io.IOException;

public class MutatedStruct implements TLSPlaintext, Handshake, ClientHello {

    private static final HandshakeType NO_HANDSHAKE_TYPE = null;

    private final int mutatedEncodingLength;
    private final byte[] mutatedEncoding;
    private final HandshakeType handshakeType;

    public MutatedStruct(byte[] mutatedEncoding) {
        this(mutatedEncoding.length, mutatedEncoding, NO_HANDSHAKE_TYPE);
    }

    public MutatedStruct(int mutatedEncodingLength, byte[] mutatedEncoding) {
        this(mutatedEncodingLength, mutatedEncoding, NO_HANDSHAKE_TYPE);
    }

    public MutatedStruct(int mutatedEncodingLength, byte[] mutatedEncoding,
            HandshakeType handshakeType) {

        this.mutatedEncodingLength = mutatedEncodingLength;
        this.mutatedEncoding = mutatedEncoding;
        this.handshakeType = handshakeType;
    }

    // Struct

    @Override
    public int encodingLength() {
        return mutatedEncodingLength;
    }

    @Override
    public byte[] encoding() throws IOException {
        return mutatedEncoding;
    }

    // TLSPlaintext

    @Override
    public boolean containsAlert() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public boolean containsApplicationData() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public boolean containsHandshake() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public byte[] getFragment() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public ProtocolVersion getLegacyRecordVersion() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public ContentType getType() {
        throw new UnsupportedOperationException("I don't know how to do that!");
    }

    @Override
    public boolean containsCertificate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsCertificateRequest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsCertificateVerify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsClientHello() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsEncryptedExtensions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsFinished() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsHelloRetryRequest() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsNewSessionTicket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsServerHello() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getBody() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HandshakeType getMessageType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UInt16 getFragmentLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UInt24 getBodyLength() {
        throw new UnsupportedOperationException();
    }


    // HandshakeMessage

    @Override
    public HandshakeType type() {
        return handshakeType;
    }

    // ClientHello

    @Override
    public Extension findExtension(ExtensionType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector<CipherSuite> getCipherSuites() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector<Extension> getExtensions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector<CompressionMethod> getLegacyCompressionMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector<Byte> getLegacySessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Random getRandom() {
        throw new UnsupportedOperationException();
    }

}
