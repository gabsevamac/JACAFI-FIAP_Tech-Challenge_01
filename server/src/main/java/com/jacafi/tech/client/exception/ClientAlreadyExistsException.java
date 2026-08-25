package com.jacafi.tech.client.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException() {
        super("A client with this tax identifier already exists");
    }
}
