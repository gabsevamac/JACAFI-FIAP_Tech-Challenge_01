package com.jacafi.tech.shared.adapter.in.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.adapter.out.persistence.AuditTrailJpaEntity;
import com.jacafi.tech.shared.adapter.out.persistence.AuditTrailJpaRepository;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxJpaEntity;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxJpaRepository;
import com.jacafi.tech.shared.adapter.out.persistence.OutboxEventStatus;
import com.jacafi.tech.shared.adapter.out.persistence.OutboxEventType;
import com.jacafi.tech.shared.application.AuditEvent;

@Component
public class AuditOutboxProcessor {
    private final EventOutboxJpaRepository outbox;
    private final AuditTrailJpaRepository auditTrail;
    private final Clock clock;

    AuditOutboxProcessor(EventOutboxJpaRepository outbox, AuditTrailJpaRepository auditTrail, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.auditTrail = Objects.requireNonNull(auditTrail, "auditTrail must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${outbox.processing-delay-ms:5000}")
    @Transactional
    public void process() {
        outbox.findTop20ByEventTypeAndStatusOrderByCreatedAtAsc(OutboxEventType.AUDIT, OutboxEventStatus.PENDING)
                .forEach(this::record);
    }

    private void record(EventOutboxJpaEntity event) {
        try {
            if (!auditTrail.existsByOutboxEventId(event.id())) {
                auditTrail.save(new AuditTrailJpaEntity(event.id(), toAuditEvent(event)));
            }
            event.markProcessed(clock.instant());
        } catch (RuntimeException exception) {
            event.markFailed(exception.getClass().getSimpleName());
        }
    }

    private static AuditEvent toAuditEvent(EventOutboxJpaEntity event) {
        Map<String, Object> payload = event.payload();
        return new AuditEvent(
                event.aggregateType(),
                event.aggregateId(),
                (String) payload.get("action"),
                (String) payload.get("actor"),
                Instant.parse((String) payload.get("occurredAt")),
                stringMap(payload.get("beforeState")),
                stringMap(payload.get("afterState")));
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return map.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue())));
    }
}
