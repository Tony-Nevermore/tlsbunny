# tlsbunny

This is a framework for building negative tests and fuzzers for TLS 1.3 implementations. The framework provides a set of basic actions which can be used in TLS 1.3 communication, for example:

- generating a ClientHello message
- wrapping a handshake message into a Handshake structure
- wrapping a handshake message into a TLSPlaintext structure
- key excnahge and generating symetric keys
- receiveing incoming data
- parsing a TLSCiphertext message
- decrypting TLSCiphertext message and so on

These basic blocks allow to control and test each step in TLS 1.3 connection.

The framework also provides an engine which runs specified actions. The engine supports adding checks and analyzers whihc run after a connection finishes.

Here is an example on HTTPS connection using TLS 1.3:

```
CommonConfig config = new CommonConfig();

        Engine.init()
                .target(config.host())
                .target(config.port())
                .send(new OutgoingClientHello())
                .send(new OutgoingChangeCipherSpec())
                .require(new IncomingServerHello())
                .require(new IncomingEncryptedExtensions())
                .require(new IncomingCertificate())
                .require(new IncomingCertificateVerify())
                .require(new IncomingFinished())
                .send(new OutgoingFinished())
                .allow(new IncomingNewSessionTicket())
                .send(new OutgoingHttpGetRequest())
                .require(new IncomingApplicationData())
                .connect()
                .run(new NoAlertCheck());
```

## Supported features

- TLS 1.3 handshake as a client
- Client authentication
- Key exchange with ECDHE using secp256r1 curve
- ecdsa_secp256r1_sha256 signatures
- AES-GCM cipher with 128-bit key

## Some test results

| Test        | OpenSSL           | GnuTLS  | picotls | wolfSSL |
| ------------- |-------------| -----|---------------|-------|
| TLSPlaintext fuzzing      |       |       |         |      |
| Handshake fuzzing        |         |       |      |      |
| ClientHello fuzzing         |         |       |      |      |
| Certificate fuzzing         |         |       |      |      |
| CertificateVerify fuzzing         |         |       |      |      |
| Finished fuzzing         |         |       |      |      |
| Double ClientHello         |         |       |      |      |
| Invalid CCS          |         |       |      |      |
| CCS after handshake is done         |         |       |      |      |
| Multiple CCS         |         |       |      |      |
| Start with CCS         |         |       |      |      |
