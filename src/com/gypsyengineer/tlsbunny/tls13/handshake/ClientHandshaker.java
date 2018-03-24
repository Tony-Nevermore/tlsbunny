package com.gypsyengineer.tlsbunny.tls13.handshake;

import com.gypsyengineer.tlsbunny.tls.UInt16;
import com.gypsyengineer.tlsbunny.tls.Vector;
import com.gypsyengineer.tlsbunny.tls13.crypto.*;
import com.gypsyengineer.tlsbunny.tls13.struct.Certificate;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateRequest;
import com.gypsyengineer.tlsbunny.tls13.struct.CertificateVerify;
import com.gypsyengineer.tlsbunny.tls13.struct.ChangeCipherSpec;
import com.gypsyengineer.tlsbunny.tls13.struct.CipherSuite;
import com.gypsyengineer.tlsbunny.tls13.struct.ClientHello;
import com.gypsyengineer.tlsbunny.tls13.struct.ContentType;
import com.gypsyengineer.tlsbunny.tls13.struct.Extension;
import com.gypsyengineer.tlsbunny.tls13.struct.ExtensionType;
import com.gypsyengineer.tlsbunny.tls13.struct.Finished;
import com.gypsyengineer.tlsbunny.tls13.struct.Handshake;
import com.gypsyengineer.tlsbunny.tls13.struct.HelloRetryRequest;
import com.gypsyengineer.tlsbunny.tls13.struct.KeyShare;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroup;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroupList;
import com.gypsyengineer.tlsbunny.tls13.struct.ProtocolVersion;
import com.gypsyengineer.tlsbunny.tls13.struct.ServerHello;
import com.gypsyengineer.tlsbunny.tls13.struct.SignatureScheme;
import com.gypsyengineer.tlsbunny.tls13.struct.SignatureSchemeList;
import com.gypsyengineer.tlsbunny.tls13.struct.StructFactory;
import com.gypsyengineer.tlsbunny.tls13.struct.SupportedVersions;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSInnerPlaintext;
import static com.gypsyengineer.tlsbunny.tls13.struct.TLSInnerPlaintext.NO_PADDING;
import com.gypsyengineer.tlsbunny.tls13.struct.TLSPlaintext;
import com.gypsyengineer.tlsbunny.utils.CertificateHolder;
import com.gypsyengineer.tlsbunny.utils.Connection;
import com.gypsyengineer.tlsbunny.utils.Utils;
import static com.gypsyengineer.tlsbunny.utils.Utils.concatenate;
import static com.gypsyengineer.tlsbunny.utils.Utils.info;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

@Deprecated
public class ClientHandshaker extends AbstractHandshaker {

    private final CertificateHolder clientCertificate;
    private Vector<Byte> certificate_request_context;

    ClientHandshaker(StructFactory factory, SignatureScheme scheme, NamedGroup group,
            Negotiator negotiator, CipherSuite ciphersuite, HKDF hkdf,
            CertificateHolder clientCertificate) {

        super(factory, scheme, group, negotiator, ciphersuite, hkdf);
        this.clientCertificate = clientCertificate;
    }

    @Override
    public void reset() {
        super.reset();
        certificate_request_context = null;
    }

    @Override
    public ClientHello createClientHello() throws Exception {
        List<Extension> extensions = List.of(
                wrap(factory.createSupportedVersionForClientHello(ProtocolVersion.TLSv13)),
                wrap(factory.createSignatureSchemeList(scheme)),
                wrap(factory.createNamedGroupList(group)),
                wrap(factory.createKeyShareForClientHello(negotiator.createKeyShareEntry())));

        ClientHello hello = factory.createClientHello(ProtocolVersion.TLSv12,
                createRandom(),
                StructFactory.EMPTY_SESSION_ID,
                List.of(CipherSuite.TLS_AES_128_GCM_SHA256),
                List.of(factory.createCompressionMethod(0)),
                extensions);

        return hello;
    }

    @Override
    public Certificate createCertificate() throws IOException {
        return factory.createCertificate(
                certificate_request_context.bytes(),
                factory.createX509CertificateEntry(clientCertificate.getCertData()));
    }

    @Override
    public CertificateVerify createCertificateVerify() throws Exception {
        byte[] content = Utils.concatenate(
                CERTIFICATE_VERIFY_PREFIX,
                CERTIFICATE_VERIFY_CONTEXT_STRING,
                new byte[] { 0 },
                TranscriptHash.compute(ciphersuite.hash(), context.allMessages()));

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(
                KeyFactory.getInstance("EC").generatePrivate(
                        new PKCS8EncodedKeySpec(clientCertificate.getKeyData())));
        signature.update(content);

        return factory.createCertificateVerify(
                SignatureScheme.ecdsa_secp256r1_sha256, signature.sign());
    }

    @Override
    public Finished createFinished() throws Exception {
        byte[] verify_data = hkdf.hmac(
                context.finished_key,
                TranscriptHash.compute(ciphersuite.hash(), context.allMessages()));

        return factory.createFinished(verify_data);
    }

    void computeKeysAfterClientFinished() throws Exception {
        context.resumption_master_secret = hkdf.deriveSecret(
                context.master_secret,
                res_master,
                context.allMessages());
        context.client_application_write_key = hkdf.expandLabel(
                context.client_application_traffic_secret_0,
                key,
                ZERO_HASH_VALUE,
                ciphersuite.keyLength());
        context.client_application_write_iv = hkdf.expandLabel(
                context.client_application_traffic_secret_0,
                iv,
                ZERO_HASH_VALUE,
                ciphersuite.ivLength());
        context.server_application_write_key = hkdf.expandLabel(
                context.server_application_traffic_secret_0,
                key,
                ZERO_HASH_VALUE,
                ciphersuite.keyLength());
        context.server_application_write_iv = hkdf.expandLabel(
                context.server_application_traffic_secret_0,
                iv,
                ZERO_HASH_VALUE,
                ciphersuite.ivLength());
    }

    private void handleHelloRetryRequest(Handshake handshake) {
        HelloRetryRequest helloRetryRequest = parser.parseHelloRetryRequest(
                handshake.getBody());
        context.setHelloRetryRequest(handshake);
    }

    void handleServerHello(Handshake handshake) throws Exception {
        ServerHello serverHello = parser.parseServerHello(handshake.getBody());

        if (!ciphersuite.equals(serverHello.getCipherSuite())) {
            throw new RuntimeException("unexpected ciphersuite");
        }

        SupportedVersions.ServerHello selected_version = findSupportedVersion(serverHello);
        if (!selected_version.equals(ProtocolVersion.TLSv13)) {
            info("server hello, selected version: %s", selected_version);
            // TODO: when TLS 1.3 spec is finished, we should throw an exception here
        }

        KeyShare.ServerHello keyShare = findKeyShare(serverHello);
        if (!group.equals(keyShare.getServerShare().getNamedGroup())) {
            info("expected group: %s", group);
            info("received group: %s", keyShare.getServerShare().getNamedGroup());
            throw new RuntimeException("unexpected group");
        }

        negotiator.processKeyShareEntry(keyShare.getServerShare());
        context.dh_shared_secret = negotiator.generateSecret();

        context.setServerHello(handshake);

        byte[] psk = zeroes(hkdf.getHashLength());

        Handshake wrappedClientHello = context.getFirstClientHello();

        context.early_secret = hkdf.extract(ZERO_SALT, psk);
        context.binder_key = hkdf.deriveSecret(
                context.early_secret,
                concatenate(ext_binder, res_binder));
        context.client_early_traffic_secret = hkdf.deriveSecret(
                context.early_secret,
                c_e_traffic,
                wrappedClientHello);
        context.early_exporter_master_secret = hkdf.deriveSecret(
                context.early_secret,
                e_exp_master,
                wrappedClientHello);

        context.handshake_secret_salt = hkdf.deriveSecret(
                context.early_secret, derived);

        context.handshake_secret = hkdf.extract(
                context.handshake_secret_salt, context.dh_shared_secret);
        context.client_handshake_traffic_secret = hkdf.deriveSecret(
                context.handshake_secret,
                c_hs_traffic,
                wrappedClientHello, handshake);
        context.server_handshake_traffic_secret = hkdf.deriveSecret(
                context.handshake_secret,
                s_hs_traffic,
                wrappedClientHello, handshake);
        context.master_secret = hkdf.extract(
                hkdf.deriveSecret(context.handshake_secret, derived),
                zeroes(hkdf.getHashLength()));

        context.client_handshake_write_key = hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                key,
                ZERO_HASH_VALUE,
                ciphersuite.keyLength());
        context.client_handshake_write_iv = hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                iv,
                ZERO_HASH_VALUE,
                ciphersuite.ivLength());
        context.server_handshake_write_key = hkdf.expandLabel(
                context.server_handshake_traffic_secret,
                key,
                ZERO_HASH_VALUE,
                ciphersuite.keyLength());
        context.server_handshake_write_iv = hkdf.expandLabel(
                context.server_handshake_traffic_secret,
                iv,
                ZERO_HASH_VALUE,
                ciphersuite.ivLength());
        context.finished_key = hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                finished,
                ZERO_HASH_VALUE,
                hkdf.getHashLength());

        context.handshakeEncryptor = AEAD.createEncryptor(
                ciphersuite.cipher(),
                context.client_handshake_write_key,
                context.client_handshake_write_iv);
        context.handshakeDecryptor = AEAD.createDecryptor(
                ciphersuite.cipher(),
                context.server_handshake_write_key,
                context.server_handshake_write_iv);
    }

    private void handleCertificateRequest(Handshake handshake) {
        CertificateRequest certificateRequest = parser.parseCertificateRequest(
                handshake.getBody());
        certificate_request_context = certificateRequest.getCertificateRequestContext();
        context.setServerCertificateRequest(handshake);
    }

    private void handleCertificate(Handshake handshake) {
        parser.parseCertificate(
                handshake.getBody(),
                buf -> parser.parseX509CertificateEntry(buf));
        context.setServerCertificate(handshake);
    }

    private void handleCertificateVerify(Handshake handshake) {
        parser.parseCertificateVerify(handshake.getBody());
        context.setServerCertificateVerify(handshake);
    }

    private void handleEncryptedExtensions(Handshake handshake) {
        parser.parseEncryptedExtensions(handshake.getBody());
        context.setEncryptedExtensions(handshake);
    }

    private void handleFinished(Handshake handshake) throws Exception {
        Finished message = parser.parseFinished(
                handshake.getBody(),
                ciphersuite.hashLength());

        byte[] verify_key = hkdf.expandLabel(
                context.server_handshake_traffic_secret,
                finished,
                ZERO_HASH_VALUE,
                hkdf.getHashLength());

        byte[] verify_data = hkdf.hmac(
                verify_key,
                TranscriptHash.compute(ciphersuite.hash(), context.allMessages()));

        boolean success = Arrays.equals(verify_data, message.getVerifyData());
        if (!success) {
            throw new RuntimeException();
        }

        context.setServerFinished(handshake);

        context.client_application_traffic_secret_0 = hkdf.deriveSecret(
                context.master_secret,
                c_ap_traffic,
                context.allMessages());
        context.server_application_traffic_secret_0 = hkdf.deriveSecret(
                context.master_secret,
                s_ap_traffic,
                context.allMessages());
        context.exporter_master_secret = hkdf.deriveSecret(
                context.master_secret,
                exp_master,
                context.allMessages());
    }

    @Override
    public void handle(TLSPlaintext tlsPlaintext) throws Exception {
        ContentType type;
        byte[] content;

        if (tlsPlaintext.containsApplicationData()) {
            TLSInnerPlaintext tlsInnerPlaintext = parser.parseTLSInnerPlaintext(
                    decrypt(tlsPlaintext));
            type = tlsInnerPlaintext.getType();
            content = tlsInnerPlaintext.getContent();
        } else {
            type = tlsPlaintext.getType();
            content = tlsPlaintext.getFragment();
        }

        if (ContentType.alert.equals(type)) {
            receivedAlert = parser.parseAlert(tlsPlaintext.getFragment());
        } else if (ContentType.handshake.equals(type)) {
            ByteBuffer buffer = ByteBuffer.wrap(content);
            while (buffer.remaining() > 0) {
                handle(parser.parseHandshake(buffer));
            }
        } else if (ContentType.change_cipher_spec.equals(type)) {
            ChangeCipherSpec ccs = parser.parseChangeCipherSpec(content);
            if (!ccs.isValid()) {
                throw new RuntimeException();
            }
        } else if (ContentType.invalid.equals(type)) {
            throw new RuntimeException("received content type: invalid");
        } else {
            throw new RuntimeException(String.format("unexpected type: %s", type));
        }
    }

    void handle(Handshake handshake) throws Exception {
        if (handshake.containsHelloRetryRequest()) {
            handleHelloRetryRequest(handshake);
        } else if (handshake.containsServerHello()) {
            handleServerHello(handshake);
        } else if (handshake.containsEncryptedExtensions()) {
            handleEncryptedExtensions(handshake);
        } else if (handshake.containsCertificateRequest()) {
            handleCertificateRequest(handshake);
        } else if (handshake.containsCertificate()) {
            handleCertificate(handshake);
        } else if (handshake.containsCertificateVerify()) {
            handleCertificateVerify(handshake);
        } else if (handshake.containsFinished()) {
            handleFinished(handshake);
        } else {
            throw new RuntimeException();
        }
    }

    TLSPlaintext[] encrypt(Handshake message) throws Exception {
        return factory.createTLSPlaintexts(
                ContentType.application_data,
                ProtocolVersion.TLSv12,
                encrypt(message.encoding()));
    }

    private byte[] encrypt(byte[] data) throws Exception {
        TLSInnerPlaintext tlsInnerPlaintext = factory.createTLSInnerPlaintext(
                ContentType.handshake, data, NO_PADDING);
        byte[] plaintext = tlsInnerPlaintext.encoding();

        int length = plaintext.length + AesGcm.TAG_LENGTH_IN_BYTES;
        byte[] additional_data = AEAD.getAdditionalData(length);

        context.handshakeEncryptor.start();
        context.handshakeEncryptor.updateAAD(additional_data);
        context.handshakeEncryptor.update(plaintext);
        byte[] ciphertext = context.handshakeEncryptor.finish();

        if (length != ciphertext.length) {
            info("actual length: %d", length);
            info("expected length: %d", ciphertext.length);
            throw new RuntimeException("wrong additional data");
        }

        return ciphertext;
    }

    byte[] decrypt(TLSPlaintext tlsCiphertext) throws Exception {
        return decrypt(tlsCiphertext.getFragment(), AEAD.getAdditionalData(tlsCiphertext));
    }

    private byte[] decrypt(byte[] ciphertext, byte[] additional_data) throws Exception {
        context.handshakeDecryptor.start();
        context.handshakeDecryptor.updateAAD(additional_data);
        context.handshakeDecryptor.update(ciphertext);
        return context.handshakeDecryptor.finish();
    }

    @Override
    public ApplicationDataChannel start(Connection connection) throws Exception {
        reset();

        Handshake handshake = toHandshake(createClientHello());
        connection.send(factory.createTLSPlaintexts(ContentType.handshake,
                ProtocolVersion.TLSv12,
                handshake.encoding()));

        context.setFirstClientHello(handshake);

        ByteBuffer buffer = ByteBuffer.wrap(connection.read());
        if (buffer.remaining() == 0) {
            return NO_APPLICATION_DATA_CHANNEL;
        }

        while (buffer.remaining() > 0) {
            TLSPlaintext tlsPlaintext = parser.parseTLSPlaintext(buffer);
            handle(tlsPlaintext);
        }

        if (receivedAlert()) {
            return NO_APPLICATION_DATA_CHANNEL;
        }

        if (requestedClientAuth()) {
            Certificate certificate = createCertificate();
            handshake = toHandshake(certificate);
            connection.send(encrypt(handshake));
            context.setClientCertificate(handshake);

            CertificateVerify certificateVerify = createCertificateVerify();
            handshake = toHandshake(certificateVerify);
            connection.send(encrypt(handshake));
            context.setClientCertificateVerify(handshake);
        }

        Finished clientFinished = createFinished();
        handshake = toHandshake(clientFinished);
        context.setClientFinished(handshake);
        computeKeysAfterClientFinished();
        connection.send(encrypt(handshake));

        applicationData = createApplicationDataChannel(connection);

        buffer = ByteBuffer.wrap(connection.read());
        if (buffer.remaining() == 0) {
            return NO_APPLICATION_DATA_CHANNEL;
        }

        TLSInnerPlaintext tlsInnerPlaintext =
                applicationData.decrypt(parser.parseTLSPlaintext(buffer));

        if (tlsInnerPlaintext.containsHandshake()) {
            handshake = parser.parseHandshake(tlsInnerPlaintext.getContent());
            if (!handshake.containsNewSessionTicket()) {
                return NO_APPLICATION_DATA_CHANNEL;
            }

            // TODO: handle NewSessionTicket
        }

        if (tlsInnerPlaintext.containsApplicationData()) {
            throw new RuntimeException("Oops, I can't handle application data!");
        }

        return applicationData;
    }

    private Extension wrap(SupportedVersions supportedVersions) throws IOException {
        return factory.createExtension(
                ExtensionType.supported_versions, supportedVersions.encoding());
    }

    private Extension wrap(SignatureSchemeList signatureSchemeList) throws IOException {
        return factory.createExtension(
                ExtensionType.signature_algorithms, signatureSchemeList.encoding());
    }

    private Extension wrap(NamedGroupList namedGroupList) throws IOException {
        return factory.createExtension(
                ExtensionType.supported_groups, namedGroupList.encoding());
    }

    private Extension wrap(KeyShare keyShare) throws IOException {
        return factory.createExtension(
                ExtensionType.key_share, keyShare.encoding());
    }

    private boolean requestedClientAuth() {
        return certificate_request_context != null;
    }

    private KeyShare.ServerHello findKeyShare(ServerHello hello) throws IOException {
        return parser.parseKeyShareFromServerHello(
                hello.findExtension(ExtensionType.key_share)
                        .getExtensionData().bytes());
    }

    private SupportedVersions.ServerHello findSupportedVersion(ServerHello hello) throws IOException {
        return parser.parseSupportedVersionsServerHello(
                hello.findExtension(ExtensionType.supported_versions)
                        .getExtensionData().bytes());
    }

    public static ClientHandshaker create(StructFactory factory,
            SignatureScheme scheme, NamedGroup group,
            Negotiator negotiator, CipherSuite ciphersuite,
            CertificateHolder clientCertificate) throws Exception {

        return new ClientHandshaker(
                factory, scheme, group, negotiator, ciphersuite,
                HKDF.create(ciphersuite.hash(), factory),
                clientCertificate);
    }

    private static byte[] zeroes(int length) {
        return new byte[length];
    }

}
