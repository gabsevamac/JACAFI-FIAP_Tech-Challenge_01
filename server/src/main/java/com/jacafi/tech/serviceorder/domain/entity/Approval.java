package com.jacafi.tech.serviceorder.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record Approval(String idempotencyKey, UUID estimateId, Instant approvedAt) {
    public Approval {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new IllegalArgumentException("idempotencyKey must contain between 1 and 120 characters");
        }
        if (estimateId == null || approvedAt == null) {
            throw new IllegalArgumentException("approval fields must not be null");
        }
    }
}
