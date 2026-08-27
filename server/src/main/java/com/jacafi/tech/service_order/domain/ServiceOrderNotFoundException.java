package com.jacafi.tech.service_order.domain;

import java.util.UUID;

public class ServiceOrderNotFoundException extends RuntimeException {

    public ServiceOrderNotFoundException(UUID id) {
        super("Service order with id " + id + " was not found.");
    }
}
