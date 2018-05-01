package com.gypsyengineer.tlsbunny.tls13.handshake;

import com.gypsyengineer.tlsbunny.tls13.struct.KeyShareEntry;
import com.gypsyengineer.tlsbunny.tls13.struct.NamedGroup;
import com.gypsyengineer.tlsbunny.tls13.struct.StructFactory;

public interface Negotiator {

    KeyShareEntry createKeyShareEntry() throws NegotiatorException;
    void processKeyShareEntry(KeyShareEntry entry) throws NegotiatorException;
    byte[] generateSecret();

    static Negotiator create(NamedGroup group, StructFactory factory)
            throws NegotiatorException {
        
        if (group instanceof NamedGroup.FFDHE) {
            return FFDHENegotiator.create((NamedGroup.FFDHE) group, factory);
        }

        if (group instanceof NamedGroup.Secp ) {
            return ECDHENegotiator.create((NamedGroup.Secp) group, factory);
        }

        throw new IllegalArgumentException();
    }

}