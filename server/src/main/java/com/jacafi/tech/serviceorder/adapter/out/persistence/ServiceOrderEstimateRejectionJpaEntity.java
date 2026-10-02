package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_order_estimate_rejections")
class ServiceOrderEstimateRejectionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "estimate_id", nullable = false, updatable = false)
    private UUID estimateId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "rejected_at", nullable = false, updatable = false)
    private Instant rejectedAt;

    protected ServiceOrderEstimateRejectionJpaEntity() {}

    ServiceOrderEstimateRejectionJpaEntity(
            UUID serviceOrderId, UUID estimateId, String idempotencyKey, Instant rejectedAt) {
        this.serviceOrderId = serviceOrderId;
        this.estimateId = estimateId;
        this.idempotencyKey = idempotencyKey;
        this.rejectedAt = rejectedAt;
    }

    UUID estimateId() {
        return estimateId;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    Instant rejectedAt() {
        return rejectedAt;
    }
}
