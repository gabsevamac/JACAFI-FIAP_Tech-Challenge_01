package com.jacafi.tech.customer.application.service;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class GetCurrentCustomerService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public GetCurrentCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer get() {
        return customers.findById(access.currentCustomerId()).orElseThrow(CustomerNotFoundException::new);
    }
}
