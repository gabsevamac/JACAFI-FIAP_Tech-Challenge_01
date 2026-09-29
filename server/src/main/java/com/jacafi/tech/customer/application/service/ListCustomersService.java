package com.jacafi.tech.customer.application.service;

import java.util.Objects;

import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public final class ListCustomersService {

    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;

    public ListCustomersService(CustomerRepository customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public PageResult<Customer> list(Boolean active, PageQuery query) {
        access.requireEmployee();
        return customers.findAll(active, Objects.requireNonNull(query, "query must not be null"));
    }
}
