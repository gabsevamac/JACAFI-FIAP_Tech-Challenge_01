package com.jacafi.tech.customer.exception;

public class InvalidTaxIdException extends IllegalArgumentException {

    public InvalidTaxIdException() {
        super("Invalid tax id");
    }
}
