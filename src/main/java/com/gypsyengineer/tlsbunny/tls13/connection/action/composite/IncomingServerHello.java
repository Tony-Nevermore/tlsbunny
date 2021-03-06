package com.gypsyengineer.tlsbunny.tls13.connection.action.composite;

import com.gypsyengineer.tlsbunny.tls13.connection.action.AbstractAction;
import com.gypsyengineer.tlsbunny.tls13.connection.action.Action;
import com.gypsyengineer.tlsbunny.tls13.connection.action.ActionFailed;
import com.gypsyengineer.tlsbunny.tls13.crypto.AEAD;
import com.gypsyengineer.tlsbunny.tls13.crypto.AEADException;
import com.gypsyengineer.tlsbunny.tls13.handshake.NegotiatorException;
import com.gypsyengineer.tlsbunny.tls13.struct.*;

import static com.gypsyengineer.tlsbunny.tls13.handshake.Context.ZERO_HASH_VALUE;
import static com.gypsyengineer.tlsbunny.tls13.handshake.Context.ZERO_SALT;
import static com.gypsyengineer.tlsbunny.tls13.utils.TLS13Utils.findKeyShare;
import static com.gypsyengineer.tlsbunny.tls13.utils.TLS13Utils.findSupportedVersion;
import static com.gypsyengineer.tlsbunny.utils.Utils.concatenate;
import static com.gypsyengineer.tlsbunny.utils.Utils.zeroes;

import java.io.IOException;

public class IncomingServerHello extends AbstractAction {

    @Override
    public String name() {
        return "ServerHello";
    }

    @Override
    public Action run()
            throws ActionFailed, NegotiatorException, IOException, AEADException {

        TLSPlaintext tlsPlaintext = context.factory.parser().parseTLSPlaintext(in);

        if (tlsPlaintext.containsAlert()) {
            Alert alert = context.factory.parser().parseAlert(tlsPlaintext.getFragment());
            context.setAlert(alert);
            throw new ActionFailed(String.format("received an alert: %s", alert));
        }

        if (!tlsPlaintext.containsHandshake()) {
            throw new ActionFailed("expected a handshake message");
        }

        Handshake handshake = context.factory.parser().parseHandshake(tlsPlaintext.getFragment());
        if (!handshake.containsServerHello()) {
            throw new ActionFailed("expected a ServerHello message");
        }

        processServerHello(handshake);

        return this;
    }

    private void processServerHello(Handshake handshake) throws ActionFailed, IOException, NegotiatorException, AEADException {
        ServerHello serverHello = context.factory.parser().parseServerHello(handshake.getBody());

        if (!context.suite.equals(serverHello.getCipherSuite())) {
            output.info("expected cipher suite: %s", context.suite);
            output.info("received cipher suite: %s", serverHello.getCipherSuite());
            throw new ActionFailed("unexpected ciphersuite");
        }

        SupportedVersions.ServerHello selected_version = findSupportedVersion(
                context.factory, serverHello);
        if (!selected_version.equals(ProtocolVersion.TLSv13)) {
            output.info("ServerHello.selected version: %s",
                    selected_version.getSelectedVersion());
            // TODO: when TLSBUNNY 1.3 spec is finished, we should throw an exception here
        }

        // TODO: we look for only first key share, but there may be multiple key shares
        KeyShare.ServerHello keyShare = findKeyShare(context.factory, serverHello);
        if (!context.group.equals(keyShare.getServerShare().getNamedGroup())) {
            output.info("expected group: %s", context.group);
            output.info("received group: %s", keyShare.getServerShare().getNamedGroup());
            throw new RuntimeException("unexpected group");
        }

        context.negotiator.processKeyShareEntry(keyShare.getServerShare());
        context.dh_shared_secret = context.negotiator.generateSecret();

        context.setServerHello(handshake);

        byte[] psk = zeroes(context.hkdf.getHashLength());

        Handshake wrappedClientHello = context.getFirstClientHello();

        context.early_secret = context.hkdf.extract(ZERO_SALT, psk);
        context.binder_key = context.hkdf.deriveSecret(
                context.early_secret,
                concatenate(context.ext_binder, context.res_binder));
        context.client_early_traffic_secret = context.hkdf.deriveSecret(
                context.early_secret,
                context.c_e_traffic,
                wrappedClientHello);
        context.early_exporter_master_secret = context.hkdf.deriveSecret(
                context.early_secret,
                context.e_exp_master,
                wrappedClientHello);

        context.handshake_secret_salt = context.hkdf.deriveSecret(
                context.early_secret, context.derived);

        context.handshake_secret = context.hkdf.extract(
                context.handshake_secret_salt, context.dh_shared_secret);
        context.client_handshake_traffic_secret = context.hkdf.deriveSecret(
                context.handshake_secret,
                context.c_hs_traffic,
                wrappedClientHello, handshake);
        context.server_handshake_traffic_secret = context.hkdf.deriveSecret(
                context.handshake_secret,
                context.s_hs_traffic,
                wrappedClientHello, handshake);
        context.master_secret = context.hkdf.extract(
                context.hkdf.deriveSecret(context.handshake_secret, context.derived),
                zeroes(context.hkdf.getHashLength()));

        context.client_handshake_write_key = context.hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                context.key,
                ZERO_HASH_VALUE,
                context.suite.keyLength());
        context.client_handshake_write_iv = context.hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                context.iv,
                ZERO_HASH_VALUE,
                context.suite.ivLength());
        context.server_handshake_write_key = context.hkdf.expandLabel(
                context.server_handshake_traffic_secret,
                context.key,
                ZERO_HASH_VALUE,
                context.suite.keyLength());
        context.server_handshake_write_iv = context.hkdf.expandLabel(
                context.server_handshake_traffic_secret,
                context.iv,
                ZERO_HASH_VALUE,
                context.suite.ivLength());
        context.finished_key = context.hkdf.expandLabel(
                context.client_handshake_traffic_secret,
                context.finished,
                ZERO_HASH_VALUE,
                context.hkdf.getHashLength());

        context.handshakeEncryptor = AEAD.createEncryptor(
                context.suite.cipher(),
                context.client_handshake_write_key,
                context.client_handshake_write_iv);
        context.handshakeDecryptor = AEAD.createDecryptor(
                context.suite.cipher(),
                context.server_handshake_write_key,
                context.server_handshake_write_iv);
    }

}
