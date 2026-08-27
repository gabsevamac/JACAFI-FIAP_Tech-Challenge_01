package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    boolean existsByTaxId(String taxId);

    Optional<CustomerJpaEntity> findByTaxId(String taxId);

    Page<CustomerJpaEntity> findAllByActive(boolean active, Pageable pageable);
}
