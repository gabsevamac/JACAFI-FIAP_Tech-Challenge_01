package com.jacafi.tech.inventory.adapter.out.persistence;

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

import com.jacafi.tech.inventory.domain.entity.AuditedOperation;

@Entity
@Table(name = "inventory_audit_entries")
class InventoryAuditEntryJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, updatable = false)
    private AuditedOperation operation;

    @Column(name = "service_order_id", updatable = false)
    private UUID serviceOrderId;

    @Column(name = "quantity", updatable = false)
    private Integer quantity;

    @Column(name = "actor", nullable = false, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected InventoryAuditEntryJpaEntity() {}

    InventoryAuditEntryJpaEntity(
            UUID itemId, AuditedOperation operation, UUID orderId, Integer quantity, String actor, Instant occurredAt) {
        this.inventoryItemId = itemId;
        this.operation = operation;
        this.serviceOrderId = orderId;
        this.quantity = quantity;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    UUID inventoryItemId() {
        return inventoryItemId;
    }

    AuditedOperation operation() {
        return operation;
    }

    UUID serviceOrderId() {
        return serviceOrderId;
    }

    Integer quantity() {
        return quantity;
    }

    String actor() {
        return actor;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
