package com.jacafi.tech.customer.exception;

public class CustomerAlreadyExistsException extends RuntimeException {

    public CustomerAlreadyExistsException() {
        super("A customer with this tax id already exists");
    }
}
