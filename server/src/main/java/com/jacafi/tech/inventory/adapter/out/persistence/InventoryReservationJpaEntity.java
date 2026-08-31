package com.jacafi.tech.inventory.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "inventory_reservations")
class InventoryReservationJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_at", nullable = false, updatable = false)
    private Instant reservedAt;

    protected InventoryReservationJpaEntity() {}

    InventoryReservationJpaEntity(
            UUID id, UUID inventoryItemId, UUID serviceOrderId, int quantity, Instant reservedAt) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.serviceOrderId = serviceOrderId;
        this.quantity = quantity;
        this.reservedAt = reservedAt;
    }

    void release(Instant at, String actor) {
        if (!isDeleted()) markDeleted(at, actor);
    }

    void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    UUID id() {
        return id;
    }

    UUID inventoryItemId() {
        return inventoryItemId;
    }

    UUID serviceOrderId() {
        return serviceOrderId;
    }

    int quantity() {
        return quantity;
    }

    Instant reservedAt() {
        return reservedAt;
    }
}
