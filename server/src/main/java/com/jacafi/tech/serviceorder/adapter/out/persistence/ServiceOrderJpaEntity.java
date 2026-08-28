package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_orders")
class ServiceOrderJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ServiceOrderStatus status;

    @Column(name = "reported_issue", nullable = false)
    private String reportedIssue;

    protected ServiceOrderJpaEntity() {}

    ServiceOrderJpaEntity(UUID id, UUID customerId, UUID vehicleId, ServiceOrderStatus status, String reportedIssue) {
        this.id = id;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.status = status;
        this.reportedIssue = reportedIssue;
    }

    void apply(ServiceOrder order) {
        status = order.status();
        reportedIssue = order.reportedIssue();
    }

    UUID id() {
        return id;
    }

    UUID customerId() {
        return customerId;
    }

    UUID vehicleId() {
        return vehicleId;
    }

    ServiceOrderStatus status() {
        return status;
    }

    String reportedIssue() {
        return reportedIssue;
    }
}
