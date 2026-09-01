package com.jacafi.tech.customer.application.service;

import java.util.Objects;
import java.util.UUID;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class UpdateCustomerService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public UpdateCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer update(UUID customerId, String name, String tradeName, String email, String phone) {
        access.requireEmployee();
        Customer customer = customers
                .findById(Objects.requireNonNull(customerId, "customerId must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
        customer.changeProfile(name, tradeName, email, phone);
        return customers.save(customer);
    }
}
