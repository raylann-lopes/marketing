package com.north.producoes.exception;

public class MetaGraphIntegrationException extends RuntimeException {

    public MetaGraphIntegrationException(String message) {
        super(message);
    }

    public MetaGraphIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
