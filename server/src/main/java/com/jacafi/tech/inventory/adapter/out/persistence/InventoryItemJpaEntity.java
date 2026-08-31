package com.jacafi.tech.inventory.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "inventory_items")
class InventoryItemJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 10)
    private MaterialType type;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_on_hand", nullable = false)
    private int stockOnHand;

    protected InventoryItemJpaEntity() {}

    InventoryItemJpaEntity(UUID id, String name, MaterialType type, BigDecimal unitPrice, int stockOnHand) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.unitPrice = unitPrice;
        this.stockOnHand = stockOnHand;
    }

    void apply(InventoryItem item, String actor) {
        name = item.name();
        unitPrice = item.unitPrice();
        stockOnHand = item.stockOnHand().value();
        if (!item.active() && !isDeleted()) markDeleted(item.removedAt().orElseThrow(), actor);
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    MaterialType type() {
        return type;
    }

    BigDecimal unitPrice() {
        return unitPrice;
    }

    int stockOnHand() {
        return stockOnHand;
    }
}
