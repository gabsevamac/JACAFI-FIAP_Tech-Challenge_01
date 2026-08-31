package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTrailJpaRepository extends JpaRepository<AuditTrailJpaEntity, Long> {
    boolean existsByOutboxEventId(UUID outboxEventId);
}
