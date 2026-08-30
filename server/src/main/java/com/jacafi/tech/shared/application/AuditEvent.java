package com.jacafi.tech.shared.application;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(
        String aggregateType,
        UUID aggregateId,
        String action,
        String actor,
        Instant occurredAt,
        Map<String, String> beforeState,
        Map<String, String> afterState) {

    public AuditEvent(String aggregateType, UUID aggregateId, String action, String actor, Instant occurredAt) {
        this(aggregateType, aggregateId, action, actor, occurredAt, Map.of(), Map.of());
    }

    public AuditEvent {
        aggregateType = requireText(aggregateType, "aggregateType");
        action = requireText(action, "action");
        actor = requireText(actor, "actor");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        beforeState = Map.copyOf(Objects.requireNonNull(beforeState, "beforeState must not be null"));
        afterState = Map.copyOf(Objects.requireNonNull(afterState, "afterState must not be null"));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
