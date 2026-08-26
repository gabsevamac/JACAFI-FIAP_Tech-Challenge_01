package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Storage shape of one audit trail line (LGPD Art. 37), which here doubles as the stock ledger.
 *
 * <p>{@code service_order_id} and {@code quantity} are nullable because catalogue operations move
 * no stock and name no order. A replenishment names a quantity but no order — the shelf was
 * filled, not promised.
 *
 * <p>Append-only: there is no setter, no update path and no delete method anywhere above it.
 */
@Entity
@Table(name = "inventory_audit_entries")
public class InventoryAuditEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, updatable = false, length = 20)
    private AuditedOperation operation;

    @Column(name = "service_order_id", updatable = false)
    private UUID serviceOrderId;

    @Column(name = "quantity", updatable = false)
    private Integer quantity;

    @Column(name = "actor", nullable = false, updatable = false, length = 120)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Required by JPA. */
    protected InventoryAuditEntryJpaEntity() {
    }

    InventoryAuditEntryJpaEntity(UUID inventoryItemId, AuditedOperation operation, UUID serviceOrderId,
                                 Integer quantity, String actor, Instant occurredAt) {
        this.inventoryItemId = inventoryItemId;
        this.operation = operation;
        this.serviceOrderId = serviceOrderId;
        this.quantity = quantity;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    Long getId() {
        return id;
    }

    UUID getInventoryItemId() {
        return inventoryItemId;
    }

    AuditedOperation getOperation() {
        return operation;
    }

    UUID getServiceOrderId() {
        return serviceOrderId;
    }

    Integer getQuantity() {
        return quantity;
    }

    String getActor() {
        return actor;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryAuditEntryJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "InventoryAuditEntryJpaEntity[id=%s, inventoryItemId=%s, operation=%s]"
                .formatted(id, inventoryItemId, operation);
    }
}
