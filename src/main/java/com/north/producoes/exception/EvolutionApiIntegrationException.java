package com.north.producoes.exception;

public class EvolutionApiIntegrationException extends RuntimeException {

    public EvolutionApiIntegrationException(String message) {
        super(message);
    }

    public EvolutionApiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
