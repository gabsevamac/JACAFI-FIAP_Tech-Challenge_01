package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.domain.MaterialType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Storage shape of a stock item. Deliberately separate from the aggregate: {@code domain/} may not
 * import {@code jakarta.persistence}, so the ORM mapping lives here and a mapper moves state
 * across. The boilerplate is the price of that boundary.
 *
 * <p>A JPA entity cannot be a record — the specification requires a no-args constructor and
 * non-final fields — so this is a plain class. It has no setters either: the mapper builds a full
 * instance and the adapter merges it, which keeps "partially updated row" from being a
 * representable state.
 *
 * <p>Reservations are not mapped as a collection here. They are rows of their own, reconciled
 * explicitly by the adapter, so that saving an aggregate never depends on how a particular ORM
 * decides to cascade or to orphan-remove.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 10)
    private MaterialType type;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Everything on the shelf, reserved units included. Never negative — see the check constraint. */
    @Column(name = "stock_on_hand", nullable = false)
    private int stockOnHand;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Null while the item is in the catalogue. Its presence is what removes a row from every query. */
    @Column(name = "removed_at")
    private Instant removedAt;

    /**
     * Required by JPA, which instantiates entities reflectively before populating their state.
     * Kept {@code protected} so only Hibernate and the mapper in this package can reach it.
     */
    protected InventoryItemJpaEntity() {
    }

    InventoryItemJpaEntity(UUID id, String name, MaterialType type, BigDecimal unitPrice,
                           int stockOnHand, Instant registeredAt, Instant updatedAt,
                           Instant removedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.unitPrice = unitPrice;
        this.stockOnHand = stockOnHand;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.removedAt = removedAt;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    MaterialType getType() {
        return type;
    }

    BigDecimal getUnitPrice() {
        return unitPrice;
    }

    int getStockOnHand() {
        return stockOnHand;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getRemovedAt() {
        return removedAt;
    }

    /** Identity-based equality: field-based equality breaks for managed entities. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItemJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "InventoryItemJpaEntity[id=%s, name=%s, onHand=%d, removed=%s]"
                .formatted(id, name, stockOnHand, removedAt != null);
    }
}
