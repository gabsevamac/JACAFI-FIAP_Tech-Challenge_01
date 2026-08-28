package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.serviceorder.domain.entity.EstimateDecision;

@Entity
@Table(name = "service_order_estimate_decisions")
class ServiceOrderEstimateDecisionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "estimate_id", nullable = false, updatable = false)
    private UUID estimateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, updatable = false, length = 10)
    private EstimateDecision decision;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ServiceOrderEstimateDecisionJpaEntity() {}

    ServiceOrderEstimateDecisionJpaEntity(
            UUID serviceOrderId,
            UUID estimateId,
            EstimateDecision decision,
            String idempotencyKey,
            Instant occurredAt) {
        this.serviceOrderId = serviceOrderId;
        this.estimateId = estimateId;
        this.decision = decision;
        this.idempotencyKey = idempotencyKey;
        this.occurredAt = occurredAt;
    }

    UUID estimateId() {
        return estimateId;
    }

    EstimateDecision decision() {
        return decision;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
