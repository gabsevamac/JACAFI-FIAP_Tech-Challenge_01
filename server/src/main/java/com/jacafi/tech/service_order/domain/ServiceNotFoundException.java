package com.jacafi.tech.service_order.domain;

import java.util.UUID;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(UUID id) {
        super("Service with id " + id + " was not found.");
    }
}
