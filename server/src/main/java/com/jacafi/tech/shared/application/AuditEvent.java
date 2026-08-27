package com.jacafi.tech.shared.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One successful business action recorded in the append-only audit trail. */
public record AuditEvent(String aggregateType, UUID aggregateId, String action, String actor, Instant occurredAt) {

    public AuditEvent {
        aggregateType = requireText(aggregateType, "aggregateType");
        action = requireText(action, "action");
        actor = requireText(actor, "actor");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
