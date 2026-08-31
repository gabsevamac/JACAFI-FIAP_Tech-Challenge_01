package com.jacafi.tech.customer.application.service;

import java.util.Objects;
import java.util.UUID;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class DeactivateCustomerService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public DeactivateCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public void deactivate(UUID customerId) {
        access.requireOperationalAccess();
        var customer = customers
                .findById(Objects.requireNonNull(customerId, "customerId must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
        customer.deactivate();
        customers.save(customer);
    }
}
