package com.jacafi.tech.features.client.domain;

public class InvalidTaxIdentifierException extends IllegalArgumentException {

    public InvalidTaxIdentifierException() {
        super("Invalid tax identifier");
    }
}
