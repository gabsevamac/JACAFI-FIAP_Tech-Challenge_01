package com.jacafi.tech.customer.application.service;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class UpdateCurrentCustomerService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public UpdateCurrentCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer update(String name, String tradeName, String email, String phone) {
        Customer customer = customers.findById(access.currentCustomerId()).orElseThrow(CustomerNotFoundException::new);
        customer.changeProfile(name, tradeName, email, phone);
        return customers.save(customer);
    }
}
