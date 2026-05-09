package com.north.producoes.exception;

public class ApifyIntegrationException extends RuntimeException {

    public ApifyIntegrationException(String message) {
        super(message);
    }
    public ApifyIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

}
