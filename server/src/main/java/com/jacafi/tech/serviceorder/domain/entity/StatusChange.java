package com.jacafi.tech.serviceorder.domain.entity;

import java.time.Instant;

public record StatusChange(
        ServiceOrderStatus previousStatus, ServiceOrderStatus status, String actor, Instant occurredAt) {
    public StatusChange {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }
}
