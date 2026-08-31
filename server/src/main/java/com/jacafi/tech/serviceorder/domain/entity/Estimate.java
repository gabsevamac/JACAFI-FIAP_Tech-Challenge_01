package com.jacafi.tech.serviceorder.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class Estimate {
    private final UUID id;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private EstimateStatus status;
    private Instant respondedAt;

    private Estimate(UUID id, BigDecimal totalAmount, EstimateStatus status, Instant createdAt, Instant respondedAt) {
        if (id == null || createdAt == null || status == null) {
            throw new IllegalArgumentException("estimate fields must not be null");
        }
        if ((status == EstimateStatus.PENDING) != (respondedAt == null)) {
            throw new IllegalArgumentException("pending estimate response state is invalid");
        }
        this.id = id;
        this.totalAmount = requireAmount(totalAmount);
        this.status = status;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
    }

    public static Estimate pending(UUID id, BigDecimal totalAmount, Instant createdAt) {
        return new Estimate(id, totalAmount, EstimateStatus.PENDING, createdAt, null);
    }

    public static Estimate restore(
            UUID id, BigDecimal totalAmount, EstimateStatus status, Instant createdAt, Instant respondedAt) {
        return new Estimate(id, totalAmount, status, createdAt, respondedAt);
    }

    void decide(EstimateDecision decision, Instant respondedAt) {
        if (status != EstimateStatus.PENDING) {
            throw new IllegalStateException("An estimate can only be decided once");
        }
        if (decision == null || respondedAt == null) {
            throw new IllegalArgumentException("decision fields must not be null");
        }
        status = decision == EstimateDecision.APPROVE ? EstimateStatus.APPROVED : EstimateStatus.REJECTED;
        this.respondedAt = respondedAt;
    }

    public UUID id() {
        return id;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public EstimateStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant respondedAt() {
        return respondedAt;
    }

    private static BigDecimal requireAmount(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("totalAmount must be a non-negative amount with at most two decimals");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
