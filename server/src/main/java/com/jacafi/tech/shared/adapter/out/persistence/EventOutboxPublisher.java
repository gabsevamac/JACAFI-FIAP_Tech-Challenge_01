package com.jacafi.tech.shared.adapter.out.persistence;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.application.AuditEvent;

@Component
public class EventOutboxPublisher {
    private final EventOutboxJpaRepository repository;
    private final Clock clock;

    EventOutboxPublisher(EventOutboxJpaRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAudit(AuditEvent event) {
        repository.save(new EventOutboxJpaEntity(
                OutboxEventType.AUDIT,
                event.aggregateType(),
                event.aggregateId(),
                Map.of(
                        "action", event.action(),
                        "actor", event.actor(),
                        "occurredAt", event.occurredAt().toString(),
                        "beforeState", event.beforeState(),
                        "afterState", event.afterState()),
                clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishServiceOrderStatusNotification(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status) {
        repository.save(new EventOutboxJpaEntity(
                OutboxEventType.SERVICE_ORDER_STATUS_NOTIFICATION,
                "ServiceOrder",
                serviceOrderId,
                Map.of("customerId", customerId.toString(), "status", status.name()),
                clock.instant()));
    }
}
