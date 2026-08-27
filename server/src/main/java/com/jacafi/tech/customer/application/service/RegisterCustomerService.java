package com.jacafi.tech.customer.application.service;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.customer.domain.exception.CustomerAlreadyExistsException;

public final class RegisterCustomerService {

    private final CustomerRepositoryPort customers;
    private final CustomerAccessPolicy access;

    public RegisterCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer register(String taxId, String name, String tradeName, String email, String phone) {
        access.requireOperationalAccess();
        TaxId parsedTaxId = TaxId.of(taxId);
        if (customers.existsByTaxId(parsedTaxId)) {
            throw new CustomerAlreadyExistsException();
        }
        return customers.save(Customer.register(parsedTaxId, name, tradeName, email, phone));
    }
}
