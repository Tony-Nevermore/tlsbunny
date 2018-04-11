package com.gypsyengineer.tlsbunny.tls13.connection;

public class Success extends AbstractCheck {

    @Override
    public Check run() {
        failed = connection.status() != Engine.Status.success;
        return this;
    }

    @Override
    public String name() {
        return "successful connection";
    }

}
