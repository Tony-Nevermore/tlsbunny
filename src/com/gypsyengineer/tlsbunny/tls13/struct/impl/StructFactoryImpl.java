package com.gypsyengineer.tlsbunny.tls13.struct.impl;

import com.gypsyengineer.tlsbunny.tls.Bytes;
import com.gypsyengineer.tlsbunny.tls.Random;
import com.gypsyengineer.tlsbunny.tls.UInt16;
import com.gypsyengineer.tlsbunny.tls.UInt24;
import com.gypsyengineer.tlsbunny.tls.Vector;
import com.gypsyengineer.tlsbunny.tls13.struct.Alert;
import com.gypsyengineer.tlsbunny.tls13.struct.AlertDescription;
import com.gypsyengineer.tlsbunny.tls13.struct.AlertLevel;
import com.gypsyengineer.tlsbunny.tls13.struct.Certificate;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateEntry;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateRequest;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateVerify;
import com.gypsyengineer.tlsbunny.tls13.struct.CipherSuite;
import com.gypsyengineer.tlsbunny.tls13.struct.CompressionMethod;
import com.gypsyengineer.tlsbunny.tls13.struct.ContentType;
import com.gypsyengineer.tlsbunny.tls13.struct.Extension;
import com.gypsyengineer.tlsbunny.tls13.struct.Handshake;
import com.gypsyengineer.tlsbunny.tls13.struct.HandshakeType;
import com.gypsyengineer.tlsbunny.tls13.struct.ProtocolVersion;
import com.gypsyengineer.tlsbunny.tls13.struct.StructFactory;
import com.gypsyengineer.tlsbunny.tls13.struct.SupportedVersions;
import com.gypsyengineer.tlsbunny.tls13.struct.ClientHello;
import com.gypsyengineer.tlsbunny.tls13.struct.EncryptedExtensions;
import com.gypsyengineer.tlsbunny.tls13.struct.EndOfEarlyData;
import com.gypsyengineer.tlsbunny.tls13.struct.ExtensionType;
import com.gypsyengineer.tlsbunny.tls13.struct.Finished;
import com.gypsyengineer.tlsbunny.tls13.struct.HelloRetryRequest;
import com.gypsyengineer.tlsbunny.tls13.struct.KeyShare;
import com.gypsyengineer.tlsbunny.tls13.struct.KeyShareEntry;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroup;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroupList;
import com.gypsyengineer.tlsbunny.tls13.struct.ServerHello;
import com.gypsyengineer.tlsbunny.tls13.struct.SignatureScheme;
import com.gypsyengineer.tlsbunny.tls13.struct.SignatureSchemeList;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSInnerPlaintext;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSPlaintext;
import com.gypsyengineer.tlsbunny.tls13.struct.impl.CertificateEntryImpl.RawPublicKeyImpl;
import com.gypsyengineer.tlsbunny.tls13.struct.impl.CertificateEntryImpl.X509Impl;
import com.gypsyengineer.tlsbunny.utils.Utils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

// TODO: make all implementations immutable
public class StructFactoryImpl implements StructFactory {
    
    public static StructFactory getDefault() {
        return new StructFactoryImpl();
    }

    @Override
    public TLSPlaintext createTLSPlaintext(
            ContentType type, ProtocolVersion version, byte[] content) {
        
        return new TLSPlaintextImpl(type, version, 
                new UInt16(content.length), new Bytes(content));
    }
    
    @Override
    public TLSPlaintext[] createTLSPlaintexts(
            ContentType type, ProtocolVersion version, byte[] content) {
        
        if (content.length <= TLSPlaintext.MAX_ALLOWED_LENGTH) {
            return new TLSPlaintext[] {
                createTLSPlaintext(type, version, content)
            };
        }

        byte[][] fragments = Utils.split(content, TLSPlaintext.MAX_ALLOWED_LENGTH);
        TLSPlaintext[] tlsPlaintexts = new TLSPlaintext[fragments.length];
        for (int i=0; i < fragments.length; i++) {
            tlsPlaintexts[i] = createTLSPlaintext(type, version, fragments[i]);
        }

        return tlsPlaintexts;
    }
    
    @Override
    public TLSInnerPlaintext createTLSInnerPlaintext(
            ContentType type, byte[] content, byte[] zeros) {
        
        return new TLSInnerPlaintextImpl(new Bytes(content), type, new Bytes(zeros));
    }
    
    @Override
    public Handshake createHandshake(HandshakeType type, byte[] content) {
        return new HandshakeImpl(type, new UInt24(content.length), new Bytes(content));
    }
    
    @Override
    public Alert createAlert(AlertLevel level, AlertDescription description) {
        return new AlertImpl(level, description);
    }
    
    // handshake messages below
    
    @Override
    public ClientHello createClientHello(
            ProtocolVersion legacy_version,
            Random random,
            byte[] legacy_session_id,
            List<CipherSuite> cipher_suites,
            List<CompressionMethod> legacy_compression_methods,
            List<Extension> extensions) {
        
        return new ClientHelloImpl(
                legacy_version, 
                random, 
                Vector.wrap(ClientHelloImpl.LEGACY_SESSION_ID_LENGTH_BYTES, legacy_session_id),
                Vector.wrap(ClientHelloImpl.CIPHER_SUITES_LENGTH_BYTES, cipher_suites), 
                Vector.wrap(ClientHelloImpl.LEGACY_COMPRESSION_METHODS_LENGTH_BYTES, legacy_compression_methods),
                Vector.wrap(ClientHelloImpl.EXTENSIONS_LENGTH_BYTES, extensions));
    }
    
    @Override
    public ServerHello createServerHello() {
        throw new UnsupportedOperationException("I don't know how to do it yet!");
    }
    
    @Override
    public HelloRetryRequest createHelloRetryRequest() {
        throw new UnsupportedOperationException("I don't know how to do it yet!");
    }
    
    @Override
    public EncryptedExtensions createEncryptedExtensions(Extension... extensions) {
        throw new UnsupportedOperationException("I don't know how to do it yet!");
    }
    
    @Override
    public EndOfEarlyData createEndOfEarlyData() {
        throw new UnsupportedOperationException("I don't know how to do it yet!");
    }
    
    @Override
    public CertificateVerify createCertificateVerify(
            SignatureScheme algorithm, byte[] signature) {
        
        return new CertificateVerifyImpl(
                algorithm,
                Vector.wrap(CertificateVerifyImpl.SIGNATURE_LENGTH_BYTES, signature));
    }
    
    @Override
    public CertificateRequest createCertificateRequest() {
        throw new UnsupportedOperationException("I don't know how to do it yet!");
    }
    
    @Override
    public Certificate createCertificate(
            byte[] certificate_request_context,
            CertificateEntry... certificate_list) {
        
        return new CertificateImpl(
                Vector.wrap(CertificateImpl.CONTEXT_LENGTH_BYTES, certificate_request_context),
                Vector.wrap(CertificateImpl.CERTIFICATE_LIST_LENGTH_BYTES, certificate_list));
    }
    
    @Override
    public Finished createFinished(byte[] verify_data) {
        return new FinishedImpl(new Bytes(verify_data));
    }

    @Override
    public SupportedVersions.ClientHello createSupportedVersionForClientHello(ProtocolVersion version) {
        return new SupportedVersionsImpl.ClientHelloImpl(
                Vector.wrap(SupportedVersions.ClientHello.VERSIONS_LENGTH_BYTES, version));
    }
    
    @Override
    public Extension wrap(SupportedVersions supportedVersions) throws IOException {
        return wrap(ExtensionType.supported_versions, supportedVersions.encoding());
    }
    
    @Override
    public Extension wrap(SignatureSchemeList signatureSchemeList) throws IOException {
        return wrap(ExtensionType.signature_algorithms, signatureSchemeList.encoding());
    }
    
    @Override
    public Extension wrap(NamedGroupList namedGroupList) throws IOException {
        return wrap(ExtensionType.supported_groups, namedGroupList.encoding());
    }
    
    @Override
    public Extension wrap(KeyShare keyShare) throws IOException {
        return wrap(ExtensionType.key_share, keyShare.encoding());
    }
    
    @Override
    public Extension wrap(ExtensionType type, byte[] bytes) {
        return new ExtensionImpl(
                type, 
                Vector.wrap(Extension.EXTENSION_DATA_LENGTH_BYTES, bytes));
    }

    @Override
    public CompressionMethod createCompressionMethod(int code) {
        return new CompressionMethodImpl(code);
    }

    @Override
    public KeyShare.ClientHello createKeyShareForClientHello(KeyShareEntry entry) {
        return KeyShareImpl.ClientHelloImpl.create(entry);
    }

    @Override
    public SignatureSchemeList createSignatureSchemeList(SignatureScheme scheme) {
        return new SignatureSchemeListImpl(
                Vector.wrap(SignatureSchemeList.LENGTH_BYTES, scheme));
    }

    @Override
    public NamedGroupList createNamedGroupList(NamedGroup group) {
        return NamedGroupListImpl.create(group);
    }

    @Override
    public Handshake parseHandshake(ByteBuffer buffer) {
        HandshakeTypeImpl msg_type = HandshakeTypeImpl.parse(buffer);
        UInt24 length = UInt24.parse(buffer);
        Bytes body = Bytes.parse(buffer, length.value);

        return new HandshakeImpl(msg_type, length, body);
    }

    @Override
    public ServerHello parseServerHello(ByteBuffer buffer) {
        return new ServerHelloImpl(
                parseProtocolVersion(buffer), 
                Random.parse(buffer), 
                parseCipherSuite(buffer),
                Vector.parse(
                    buffer,
                    ServerHello.EXTENSIONS_LENGTH_BYTES,
                    buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public TLSInnerPlaintext parseTLSInnerPlaintext(byte[] bytes) {
        int i = bytes.length - 1;
        while (i > 0) {
            if (bytes[i] != 0) {
                break;
            }
            
            i--;
        }

        if (i == 0) {
            throw new IllegalArgumentException();
        }

        byte[] zeros = new byte[bytes.length - 1 - i];
        byte[] content = Arrays.copyOfRange(bytes, 0, i);
        
        return new TLSInnerPlaintextImpl(
                new Bytes(content), 
                new ContentTypeImpl(bytes[i]), 
                new Bytes(zeros));
    }

    @Override
    public TLSPlaintext parseTLSPlaintext(ByteBuffer buffer) {
        ContentType type = parseContentType(buffer);
        ProtocolVersion legacy_record_version = parseProtocolVersion(buffer);
        UInt16 length = UInt16.parse(buffer);
        Bytes fragment = Bytes.parse(buffer, length.value);
        
        return new TLSPlaintextImpl(type, legacy_record_version, length, fragment);
    }

    @Override
    public ContentType parseContentType(ByteBuffer buffer) {
        return new ContentTypeImpl(buffer.get() & 0xFF);
    }

    @Override
    public ProtocolVersion parseProtocolVersion(ByteBuffer buffer) {
        return new ProtocolVersionImpl(buffer.get() & 0xFF, buffer.get() & 0xFF);
    }

    @Override
    public CipherSuite parseCipherSuite(ByteBuffer buffer) {
        return new CipherSuiteImpl(buffer.get() & 0xFF, buffer.get() & 0xFF);
    }

    @Override
    public Alert parseAlert(ByteBuffer buffer) {
        return new AlertImpl(
                parseAlertLevel(buffer), 
                parseAlertDescription(buffer));
    }

    @Override
    public AlertLevel parseAlertLevel(ByteBuffer buffer) {
        return new AlertLevelImpl(buffer.get() & 0xFF);
    }

    @Override
    public AlertDescription parseAlertDescription(ByteBuffer buffer) {
        return new AlertDescriptionImpl(buffer.get() & 0xFF);
    }

    @Override
    public Certificate parseCertificate(
            ByteBuffer buffer, Vector.ContentParser certificateEntityParser) {
        
        return new CertificateImpl(
                Vector.parse(buffer,
                    Certificate.CONTEXT_LENGTH_BYTES,
                    buf -> buf.get()), 
                Vector.parse(
                    buffer,
                    Certificate.CERTIFICATE_LIST_LENGTH_BYTES,
                    certificateEntityParser));
    }

    @Override
    public CertificateRequest parseCertificateRequest(ByteBuffer buffer) {
        return new CertificateRequestImpl(Vector.parse(
                    buffer,
                    CertificateRequest.CERTIFICATE_REQUEST_CONTEXT_LENGTH_BYTES,
                    buf -> buf.get()), 
                Vector.parse(
                    buffer,
                    CertificateRequest.EXTENSIONS_LENGTH_BYTES,
                    buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public CertificateVerify parseCertificateVerify(ByteBuffer buffer) {
        return new CertificateVerifyImpl(
                parseSignatureScheme(buffer), 
                Vector.parseOpaqueVector(buffer, CertificateVerify.SIGNATURE_LENGTH_BYTES));
    }

    @Override
    public ClientHello parseClientHello(ByteBuffer buffer) {
        ProtocolVersion legacy_version = parseProtocolVersion(buffer);
        Random random = Random.parse(buffer);
        Vector<Byte> legacy_session_id = Vector.parse(
                buffer, 
                ClientHello.LEGACY_SESSION_ID_LENGTH_BYTES, 
                buf -> buf.get());
        Vector<CipherSuite> cipher_suites = Vector.parse(
                buffer, 
                ClientHello.CIPHER_SUITES_LENGTH_BYTES, 
                buf -> parseCipherSuite(buf));
        Vector<CompressionMethod> legacy_compression_methods = Vector.parse(
                buffer, 
                ClientHello.LEGACY_COMPRESSION_METHODS_LENGTH_BYTES, 
                buf -> CompressionMethodImpl.parse(buf));
        Vector<Extension> extensions = Vector.parse(
                buffer,
                ClientHello.EXTENSIONS_LENGTH_BYTES,
                buf -> ExtensionImpl.parse(buf));

        return new ClientHelloImpl(legacy_version, random, legacy_session_id, 
                cipher_suites, legacy_compression_methods, extensions);
    }

    @Override
    public EncryptedExtensions parseEncryptedExtensions(ByteBuffer buffer) {
        return new EncryptedExtensionsImpl(
                Vector.parse(
                    buffer,
                    EncryptedExtensions.EXTENSIONS_LENGTH_BYTES,
                    buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public Finished parseFinished(ByteBuffer buffer, int hashLen) {
        byte[] verify_data = new byte[hashLen];
        buffer.get(verify_data);
        
        return new FinishedImpl(new Bytes(verify_data));
    }

    @Override
    public HelloRetryRequest parseHelloRetryRequest(ByteBuffer buffer) {
        return new HelloRetryRequestImpl(
                parseProtocolVersion(buffer), 
                parseCipherSuite(buffer), 
                Vector.parse(
                    buffer,
                    HelloRetryRequest.EXTENSIONS_LENGTH_BYTES,
                    buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public SignatureScheme parseSignatureScheme(ByteBuffer buffer) {
        return new SignatureSchemeImpl(buffer.getShort());
    }
    
    @Override
    public TLSInnerPlaintext parseTLSInnerPlaintext(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public EndOfEarlyData parseEndOfEarlyData(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SignatureSchemeList parseSignatureSchemeList(ByteBuffer buffer) {
        return new SignatureSchemeListImpl(
                Vector.parse(
                        buffer, 
                        SignatureSchemeList.LENGTH_BYTES, 
                        buf -> parseSignatureScheme(buf)));
    }

    @Override
    public SupportedVersions.ClientHello parseSupportedVersionsClientHello(ByteBuffer buffer) {
        return new SupportedVersionsImpl.ClientHelloImpl(
                    Vector.parse(
                            buffer, 
                            SupportedVersions.ClientHello.VERSIONS_LENGTH_BYTES, 
                            buf -> parseProtocolVersion(buf)));
    }

    @Override
    public CertificateEntry.X509 parseX509CertificateEntry(ByteBuffer buffer) {
        return new X509Impl(
                    Vector.parseOpaqueVector(buffer, CertificateEntry.X509.LENGTH_BYTES), 
                    Vector.parse(
                        buffer,
                        CertificateEntry.X509.EXTENSIONS_LENGTH_BYTES,
                        buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public CertificateEntry.RawPublicKey parseRawPublicKeyCertificateEntry(ByteBuffer buffer) {
        return new RawPublicKeyImpl(
                    Vector.parseOpaqueVector(
                            buffer, 
                            CertificateEntry.RawPublicKey.LENGTH_BYTES), 
                    Vector.parse(
                        buffer,
                        CertificateEntry.RawPublicKey.EXTENSIONS_LENGTH_BYTES,
                        buf -> ExtensionImpl.parse(buf)));
    }

    @Override
    public CertificateEntry.X509 createX509CertificateEntry(byte[] bytes) {
        return new X509Impl(
                    Vector.wrap(CertificateEntry.X509.LENGTH_BYTES, bytes), 
                    Vector.wrap(CertificateEntry.X509.EXTENSIONS_LENGTH_BYTES));
    }

    @Override
    public NamedGroupList parseNamedGroupList(ByteBuffer buffer) {
        Vector<NamedGroup> named_group_list = Vector.parse(buffer,
                NamedGroupList.LENGTH_BYTES,
                buf -> NamedGroupImpl.parse(buf));

        return new NamedGroupListImpl(named_group_list);
    }

    @Override
    public KeyShare.ClientHello parseKeyShareFromClientHello(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
