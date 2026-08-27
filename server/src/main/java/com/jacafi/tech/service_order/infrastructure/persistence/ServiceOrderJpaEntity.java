package com.jacafi.tech.service_order.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.jacafi.tech.service_order.domain.ServiceOrderStatus;
import com.jacafi.tech.shared.persistence.AuditableEntity;

@Entity
@Table(name = "service_orders")
public class ServiceOrderJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ServiceOrderStatus status;

    @Column(name = "total", nullable = false, precision = 38, scale = 2)
    private BigDecimal total;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LaunchedServiceJpaEntity> launchedServices = new ArrayList<>();

    protected ServiceOrderJpaEntity() {}

    public ServiceOrderJpaEntity(
            UUID id,
            ServiceOrderStatus status,
            BigDecimal total,
            UUID vehicleId,
            UUID customerId,
            Instant removedAt,
            String removedBy) {
        this.id = id;
        this.status = status;
        this.total = total;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        restoreDeletion(removedAt, removedBy);
    }

    public void applyState(ServiceOrderStatus status, BigDecimal total, Instant removedAt) {
        this.status = status;
        this.total = total;
        if (removedAt != null && !isDeleted()) {
            markDeleted(removedAt, "system");
        }
    }

    public UUID getId() {
        return id;
    }

    public ServiceOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public List<LaunchedServiceJpaEntity> getLaunchedServices() {
        return launchedServices;
    }

    public void setLaunchedServices(List<LaunchedServiceJpaEntity> items) {
        this.launchedServices.clear();
        if (items != null) {
            this.launchedServices.addAll(items);
        }
    }
}
