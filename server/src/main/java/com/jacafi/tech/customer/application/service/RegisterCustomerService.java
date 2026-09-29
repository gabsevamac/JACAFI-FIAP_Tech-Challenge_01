package com.jacafi.tech.customer.application.service;

import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.customer.domain.exception.CustomerAlreadyExistsException;

public final class RegisterCustomerService {

    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;

    public RegisterCustomerService(CustomerRepository customers, CustomerAccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public Customer register(String taxId, String name, String tradeName, String email, String phone) {
        access.requireEmployee();
        TaxId parsedTaxId = TaxId.of(taxId);
        if (customers.existsByTaxId(parsedTaxId)) {
            throw new CustomerAlreadyExistsException();
        }
        return customers.save(Customer.register(parsedTaxId, name, tradeName, email, phone));
    }
}
