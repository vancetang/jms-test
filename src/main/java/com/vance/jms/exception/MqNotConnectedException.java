package com.vance.jms.exception;

public class MqNotConnectedException extends RuntimeException {
    public MqNotConnectedException(String message) {
        super(message);
    }

    public MqNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
