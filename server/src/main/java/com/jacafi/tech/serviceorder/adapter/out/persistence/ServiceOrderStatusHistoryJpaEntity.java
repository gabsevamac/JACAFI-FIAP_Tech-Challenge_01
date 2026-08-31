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

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

@Entity
@Table(name = "service_order_status_history")
class ServiceOrderStatusHistoryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 25, updatable = false)
    private ServiceOrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false, length = 25)
    private ServiceOrderStatus status;

    @Column(name = "actor", nullable = false, updatable = false, length = 120)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ServiceOrderStatusHistoryJpaEntity() {}

    ServiceOrderStatusHistoryJpaEntity(
            UUID serviceOrderId,
            ServiceOrderStatus previousStatus,
            ServiceOrderStatus status,
            String actor,
            Instant occurredAt) {
        this.serviceOrderId = serviceOrderId;
        this.previousStatus = previousStatus;
        this.status = status;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    ServiceOrderStatus previousStatus() {
        return previousStatus;
    }

    ServiceOrderStatus status() {
        return status;
    }

    String actor() {
        return actor;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
