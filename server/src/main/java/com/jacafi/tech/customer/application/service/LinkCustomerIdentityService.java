package com.jacafi.tech.customer.application.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.customer.application.port.CustomerIdentityRepository;
import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.exception.CustomerNotFoundException;

public class LinkCustomerIdentityService {

    private final CustomerRepository customers;
    private final CustomerIdentityRepository identities;
    private final CustomerAccessPolicy access;

    public LinkCustomerIdentityService(
            CustomerRepository customers, CustomerIdentityRepository identities, CustomerAccessPolicy access) {
        this.customers = customers;
        this.identities = identities;
        this.access = access;
    }

    @Transactional
    public void link(UUID customerId, String subjectId) {
        access.requireEmployee();
        customers
                .findById(Objects.requireNonNull(customerId, "customerId must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
        identities.link(subjectId, customerId);
    }
}
