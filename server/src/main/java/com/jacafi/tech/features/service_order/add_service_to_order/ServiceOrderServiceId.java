package com.jacafi.tech.features.service_order.add_service_to_order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class ServiceOrderServiceId implements Serializable {

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    ServiceOrderServiceId(UUID serviceOrderId, UUID serviceId) {
        this.serviceOrderId = serviceOrderId;
        this.serviceId = serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceOrderServiceId that)) return false;
        return Objects.equals(serviceOrderId, that.serviceOrderId)
                && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceOrderId, serviceId);
    }
}