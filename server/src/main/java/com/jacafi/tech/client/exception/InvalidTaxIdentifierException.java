package com.jacafi.tech.client.exception;

public class InvalidTaxIdentifierException extends IllegalArgumentException {

    public InvalidTaxIdentifierException() {
        super("Invalid tax identifier");
    }
}
