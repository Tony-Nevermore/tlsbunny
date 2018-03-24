package com.gypsyengineer.tlsbunny.tls13.connection;

import com.gypsyengineer.tlsbunny.tls13.crypto.HKDF;
import com.gypsyengineer.tlsbunny.tls13.handshake.Context;
import com.gypsyengineer.tlsbunny.tls13.handshake.ECDHENegotiator;
import com.gypsyengineer.tlsbunny.tls13.handshake.Negotiator;
import com.gypsyengineer.tlsbunny.tls13.struct.CipherSuite;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroup;
import com.gypsyengineer.tlsbunny.tls13.struct.SignatureScheme;
import com.gypsyengineer.tlsbunny.tls13.struct.StructFactory;
import com.gypsyengineer.tlsbunny.utils.Connection;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static com.gypsyengineer.tlsbunny.utils.Utils.info;

public class TLSConnection {

    private static final ByteBuffer NOTHING = ByteBuffer.allocate(0);

    private enum ActionType { SEND, EXPECT, ALLOW }

    public enum Status { NOT_STARTED, RUNNING, COULD_NOT_SEND, UNEXPECTED_MESSAGE, SUCCESS }

    private final List<ActionHolder> actions = new ArrayList<>();
    private String host = "localhost";
    private int port = 443;
    private StructFactory factory = StructFactory.getDefault();
    private NamedGroup group = NamedGroup.secp256r1;
    private SignatureScheme scheme = SignatureScheme.ecdsa_secp256r1_sha256;
    private CipherSuite suite = CipherSuite.TLS_AES_128_GCM_SHA256;
    private Negotiator negotiator;
    private HKDF hkdf;
    private Status status = Status.NOT_STARTED;

    private TLSConnection() {

    }

    public TLSConnection host(String host) {
        this.host = host;
        return this;
    }

    public TLSConnection port(int port) {
        this.port = port;
        return this;
    }

    public TLSConnection set(StructFactory factory) {
        this.factory = factory;
        return this;
    }

    public TLSConnection set(SignatureScheme scheme) {
        this.scheme = scheme;
        return this;
    }

    public TLSConnection set(NamedGroup group) {
        this.group = group;
        return this;
    }

    public TLSConnection set(Negotiator negotiator) {
        this.negotiator = negotiator;
        return this;
    }

    public TLSConnection send(Action action) {
        actions.add(new ActionHolder(action, ActionType.SEND));
        return this;
    }

    public TLSConnection expect(Action action) {
        actions.add(new ActionHolder(action, ActionType.EXPECT));
        return this;
    }

    public TLSConnection allow(Action action) {
        actions.add(new ActionHolder(action, ActionType.ALLOW));
        return this;
    }

    public TLSConnection run() throws IOException {
        status = Status.RUNNING;
        try (Connection connection = Connection.create(host, port)) {
            Context context = new Context();
            context.factory = factory;
            
            ByteBuffer buffer = NOTHING;
            loop: for (ActionHolder holder : actions) {
                Action action = holder.action;

                info(action.description());

                action.set(context);
                action.set(group);
                action.set(scheme);
                action.set(suite);
                action.set(negotiator);
                action.set(factory);
                action.set(hkdf);
                action.set(connection);
                action.set(buffer);

                boolean success = false;
                try {
                    action.run();
                    success = true;
                } catch (Exception e) {
                    info("exception: ", e);
                }

                switch (holder.type) {
                    case SEND:
                        if (!success) {
                            status = Status.COULD_NOT_SEND;
                            break loop;
                        }
                        break;
                    case EXPECT:
                        if (!success) {
                            status = Status.UNEXPECTED_MESSAGE;
                            break loop;
                        }
                        break;
                    case ALLOW:
                        if (!success) {
                            // do nothing
                        }
                        break;
                    default:
                        throw new IllegalStateException(
                                String.format("unknown action type: %s", holder.type));
                }

                buffer = action.data();
            }
        }

        if (status == Status.RUNNING) {
            status = Status.SUCCESS;
        }

        return this;
    }

    public TLSConnection check(Check check) {
        check.set(this);
        check.run();
        if (check.failed()) {
            throw new RuntimeException(String.format("%s check failed", check.name()));
        }

        return this;
    }

    public TLSConnection analyze(Analyzer analyzer) {
        return this;
    }

    public Status status() {
        return status;
    }

    public static TLSConnection create() throws IOException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException {

        TLSConnection connection = new TLSConnection();
        connection.negotiator = ECDHENegotiator.create(
                (NamedGroup.Secp) connection.group, connection.factory);
        connection.hkdf = HKDF.create(
                connection.suite.hash(), connection.factory);

        return connection;
    }

    private static class ActionHolder {

        ActionHolder(Action action, ActionType type) {
            this.action = action;
            this.type = type;
        }

        Action action;
        ActionType type;
    }

}
