package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository repository;

    public CustomerPersistenceAdapter(CustomerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity candidate = CustomerPersistenceMapper.toJpa(customer);
        CustomerJpaEntity entity = repository
                .findById(customer.id())
                .map(existing -> {
                    existing.apply(candidate);
                    return existing;
                })
                .orElse(candidate);
        return CustomerPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public boolean existsByTaxId(TaxId taxId) {
        return repository.existsByTaxId(taxId.value());
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return repository.findById(id).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByTaxId(TaxId taxId) {
        return repository.findByTaxId(taxId.value()).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Customer> findAll(Boolean active, PageQuery query) {
        Pageable pageable = SpringDataPaging.toPageable(query);
        Page<CustomerJpaEntity> page =
                active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable);
        return SpringDataPaging.toPageResult(page, query, CustomerPersistenceMapper::toDomain);
    }
}
