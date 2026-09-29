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
@Table(name = "service_order_estimate_approvals")
class ServiceOrderEstimateApprovalJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "estimate_id", nullable = false, updatable = false)
    private UUID estimateId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "approved_at", nullable = false, updatable = false)
    private Instant approvedAt;

    protected ServiceOrderEstimateApprovalJpaEntity() {}

    ServiceOrderEstimateApprovalJpaEntity(
            UUID serviceOrderId, UUID estimateId, String idempotencyKey, Instant approvedAt) {
        this.serviceOrderId = serviceOrderId;
        this.estimateId = estimateId;
        this.idempotencyKey = idempotencyKey;
        this.approvedAt = approvedAt;
    }

    UUID estimateId() {
        return estimateId;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    Instant approvedAt() {
        return approvedAt;
    }
}
