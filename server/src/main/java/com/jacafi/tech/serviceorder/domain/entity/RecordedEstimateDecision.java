package com.jacafi.tech.serviceorder.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record RecordedEstimateDecision(
        String idempotencyKey, UUID estimateId, EstimateDecision decision, Instant decidedAt) {
    public RecordedEstimateDecision {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new IllegalArgumentException("idempotencyKey must contain between 1 and 120 characters");
        }
        if (estimateId == null || decision == null || decidedAt == null) {
            throw new IllegalArgumentException("recorded decision fields must not be null");
        }
    }
}
