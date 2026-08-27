package com.jacafi.tech.customer.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    boolean existsByTaxId(TaxId taxId);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByTaxId(TaxId taxId);

    PageResult<Customer> findAll(Boolean active, PageQuery query);
}
