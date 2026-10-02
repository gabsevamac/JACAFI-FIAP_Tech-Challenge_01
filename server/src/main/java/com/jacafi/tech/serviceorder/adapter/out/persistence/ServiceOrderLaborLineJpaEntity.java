package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_order_labor_lines")
class ServiceOrderLaborLineJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "labor_operation_id", nullable = false, updatable = false)
    private UUID laborOperationId;

    @Column(name = "labor_operation_name_snapshot", nullable = false, length = 120)
    private String laborOperationNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ServiceOrderLaborLineJpaEntity() {}

    ServiceOrderLaborLineJpaEntity(
            UUID id,
            UUID serviceOrderId,
            UUID laborOperationId,
            String laborOperationNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.laborOperationId = laborOperationId;
        this.laborOperationNameSnapshot = laborOperationNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }

    UUID id() {
        return id;
    }

    UUID laborOperationId() {
        return laborOperationId;
    }

    String laborOperationNameSnapshot() {
        return laborOperationNameSnapshot;
    }

    BigDecimal unitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    int quantity() {
        return quantity;
    }
}
