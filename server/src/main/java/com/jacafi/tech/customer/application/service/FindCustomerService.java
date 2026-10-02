package com.jacafi.tech.customer.application.service;

import java.util.Objects;
import java.util.UUID;

import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class FindCustomerService {

    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;

    public FindCustomerService(CustomerRepository customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer find(UUID customerId) {
        access.requireEmployee();
        return customers
                .findById(Objects.requireNonNull(customerId, "customerId must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
    }
}
