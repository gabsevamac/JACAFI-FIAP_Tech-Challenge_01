package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_order_service_lines")
class ServiceOrderServiceLineJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "service_catalog_item_id", nullable = false, updatable = false)
    private UUID serviceCatalogItemId;

    @Column(name = "service_name_snapshot", nullable = false, length = 120)
    private String serviceNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ServiceOrderServiceLineJpaEntity() {}

    ServiceOrderServiceLineJpaEntity(
            UUID id,
            UUID serviceOrderId,
            UUID serviceCatalogItemId,
            String serviceNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.serviceCatalogItemId = serviceCatalogItemId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }

    UUID id() {
        return id;
    }

    UUID serviceCatalogItemId() {
        return serviceCatalogItemId;
    }

    String serviceNameSnapshot() {
        return serviceNameSnapshot;
    }

    BigDecimal unitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    int quantity() {
        return quantity;
    }
}
