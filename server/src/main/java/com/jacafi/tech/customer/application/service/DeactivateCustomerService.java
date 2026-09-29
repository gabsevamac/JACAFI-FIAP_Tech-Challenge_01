package com.jacafi.tech.customer.application.service;

import java.util.Objects;
import java.util.UUID;

import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class DeactivateCustomerService {

    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;

    public DeactivateCustomerService(CustomerRepository customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public void deactivate(UUID customerId) {
        access.requireEmployee();
        var customer = customers
                .findById(Objects.requireNonNull(customerId, "customerId must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
        customer.deactivate();
        customers.save(customer);
    }
}
