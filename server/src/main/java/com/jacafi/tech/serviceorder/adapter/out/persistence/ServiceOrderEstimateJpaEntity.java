package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.EstimateStatus;
import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_order_estimates")
class ServiceOrderEstimateJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private EstimateStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected ServiceOrderEstimateJpaEntity() {}

    ServiceOrderEstimateJpaEntity(
            UUID id, UUID serviceOrderId, EstimateStatus status, BigDecimal totalAmount, Instant respondedAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.respondedAt = respondedAt;
    }

    void apply(Estimate estimate) {
        status = estimate.status();
        respondedAt = estimate.respondedAt();
    }

    UUID id() {
        return id;
    }

    EstimateStatus status() {
        return status;
    }

    BigDecimal totalAmount() {
        return totalAmount;
    }

    Instant respondedAt() {
        return respondedAt;
    }
}
