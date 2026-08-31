package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutboxJpaEntity, UUID> {
    List<EventOutboxJpaEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<EventOutboxJpaEntity> findTop20ByEventTypeAndStatusOrderByCreatedAtAsc(
            OutboxEventType eventType, OutboxEventStatus status);
}
