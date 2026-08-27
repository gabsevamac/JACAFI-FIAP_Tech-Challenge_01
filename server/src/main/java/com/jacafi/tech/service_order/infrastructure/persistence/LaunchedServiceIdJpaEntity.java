package com.jacafi.tech.service_order.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LaunchedServiceIdJpaEntity implements Serializable {

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    protected LaunchedServiceIdJpaEntity() {}

    public LaunchedServiceIdJpaEntity(UUID serviceOrderId, UUID serviceId) {
        this.serviceOrderId = serviceOrderId;
        this.serviceId = serviceId;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaunchedServiceIdJpaEntity that)) return false;
        return Objects.equals(serviceOrderId, that.serviceOrderId) && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceOrderId, serviceId);
    }
}
