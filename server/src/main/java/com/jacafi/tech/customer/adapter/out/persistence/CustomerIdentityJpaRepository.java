package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerIdentityJpaRepository extends JpaRepository<CustomerIdentityJpaEntity, String> {

    Optional<CustomerIdentityJpaEntity> findByCustomerId(UUID customerId);
}
