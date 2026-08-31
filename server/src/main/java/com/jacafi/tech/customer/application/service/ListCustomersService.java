package com.jacafi.tech.customer.application.service;

import java.util.Objects;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public final class ListCustomersService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public ListCustomersService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public PageResult<Customer> list(Boolean active, PageQuery query) {
        access.requireOperationalAccess();
        return customers.findAll(active, Objects.requireNonNull(query, "query must not be null"));
    }
}
