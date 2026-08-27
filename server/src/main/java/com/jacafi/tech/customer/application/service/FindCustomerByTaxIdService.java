package com.jacafi.tech.customer.application.service;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public final class FindCustomerByTaxIdService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public FindCustomerByTaxIdService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer find(String taxId) {
        access.requireOperationalAccess();
        return customers.findByTaxId(TaxId.of(taxId)).orElseThrow(CustomerNotFoundException::new);
    }
}
