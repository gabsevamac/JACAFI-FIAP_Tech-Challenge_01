package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over the trail.
 *
 * <p>Package-private, and inherits no read method beyond what {@code JpaRepository} gives: the
 * application talks to {@code AuditTrailPort}, which offers only {@code record}.
 */
public interface AuditTrailJpaRepository extends JpaRepository<AuditTrailJpaEntity, Long> {
    boolean existsByOutboxEventId(UUID outboxEventId);
}
