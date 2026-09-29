package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jacafi.tech.customer.application.port.CustomerIdentityRepositoryPort;
import com.jacafi.tech.shared.security.CustomerIdentityPort;

@Component
public class CustomerIdentityPersistenceAdapter implements CustomerIdentityPort, CustomerIdentityRepositoryPort {

    private final CustomerIdentityJpaRepository repository;

    public CustomerIdentityPersistenceAdapter(CustomerIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> customerIdBySubject(String subject) {
        return repository.findById(subject).map(CustomerIdentityJpaEntity::customerId);
    }

    @Override
    public void link(String subjectId, UUID customerId) {
        repository
                .findByCustomerId(customerId)
                .filter(existing -> !existing.subjectId().equals(subjectId))
                .ifPresent(existing -> {
                    repository.delete(existing);
                    repository.flush();
                });

        CustomerIdentityJpaEntity identity = repository
                .findById(subjectId)
                .map(existing -> {
                    existing.moveTo(customerId);
                    return existing;
                })
                .orElseGet(() -> new CustomerIdentityJpaEntity(subjectId, customerId));
        repository.save(identity);
    }
}
