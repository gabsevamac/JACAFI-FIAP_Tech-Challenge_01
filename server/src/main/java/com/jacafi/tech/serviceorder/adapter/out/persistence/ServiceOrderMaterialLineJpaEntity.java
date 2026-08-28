package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_order_material_lines")
class ServiceOrderMaterialLineJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "inventory_item_id", nullable = false, updatable = false)
    private UUID inventoryItemId;

    @Column(name = "material_name_snapshot", nullable = false, length = 120)
    private String materialNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ServiceOrderMaterialLineJpaEntity() {}

    ServiceOrderMaterialLineJpaEntity(
            UUID id,
            UUID serviceOrderId,
            UUID inventoryItemId,
            String materialNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.inventoryItemId = inventoryItemId;
        this.materialNameSnapshot = materialNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }

    UUID id() {
        return id;
    }

    UUID inventoryItemId() {
        return inventoryItemId;
    }

    String materialNameSnapshot() {
        return materialNameSnapshot;
    }

    BigDecimal unitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    int quantity() {
        return quantity;
    }
}
