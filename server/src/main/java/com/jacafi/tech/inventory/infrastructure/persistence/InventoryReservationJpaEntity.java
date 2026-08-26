package com.jacafi.tech.inventory.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Storage shape of one open reservation.
 *
 * <p>Only open ones are rows here. A reservation ends by being released or withdrawn, and what
 * happened to it is in the audit trail, which is append-only and made to be read as a ledger.
 * Keeping settled reservations in this table would mean two places telling the same story, and
 * the balance would depend on which one a query happened to consult.
 *
 * <p>{@code service_order_id} is a plain column with no foreign key: {@code ServiceOrder} belongs
 * to another slice and another aggregate, and the boundary rule of the README is that aggregates
 * reference each other by identifier only.
 */
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_at", nullable = false, updatable = false)
    private Instant reservedAt;

    /** Required by JPA. */
    protected InventoryReservationJpaEntity() {}

    InventoryReservationJpaEntity(
            UUID id, UUID inventoryItemId, UUID serviceOrderId, int quantity, Instant reservedAt) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.serviceOrderId = serviceOrderId;
        this.quantity = quantity;
        this.reservedAt = reservedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getInventoryItemId() {
        return inventoryItemId;
    }

    UUID getServiceOrderId() {
        return serviceOrderId;
    }

    int getQuantity() {
        return quantity;
    }

    Instant getReservedAt() {
        return reservedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryReservationJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "InventoryReservationJpaEntity[id=%s, inventoryItemId=%s, serviceOrderId=%s, quantity=%d]"
                .formatted(id, inventoryItemId, serviceOrderId, quantity);
    }
}
