package com.jacafi.tech.service_order.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrderJpaEntity, UUID> {

    Optional<ServiceOrderJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
