package com.jacafi.tech.features.client.create;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException() {
        super("A client with this tax identifier already exists");
    }
}
