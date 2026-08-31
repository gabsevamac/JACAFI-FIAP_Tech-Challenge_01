package com.jacafi.tech.auth.application.port;

public final class InvalidAccessTokenException extends RuntimeException {
    public InvalidAccessTokenException(Throwable cause) {
        super(cause);
    }
}
