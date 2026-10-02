package com.jacafi.tech.serviceorder.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record Rejection(String idempotencyKey, UUID estimateId, Instant rejectedAt) {
    public Rejection {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new IllegalArgumentException("idempotencyKey must contain between 1 and 120 characters");
        }
        if (estimateId == null || rejectedAt == null) {
            throw new IllegalArgumentException("rejection fields must not be null");
        }
    }
}
