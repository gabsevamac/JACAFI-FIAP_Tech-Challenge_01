package com.jacafi.tech.features.service_order.add_service_to_order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ServiceOrderServiceId implements Serializable {

    @Column(name = "service_order_id", nullable = false, updatable = false)
    private UUID serviceOrderId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    /**
     * Required by JPA: an embeddable identifier is instantiated reflectively and populated
     * field by field. A record cannot be used for the same reason.
     */
    protected ServiceOrderServiceId() {
    }

    ServiceOrderServiceId(UUID serviceOrderId, UUID serviceId) {
        this.serviceOrderId = serviceOrderId;
        this.serviceId = serviceId;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    /** A composite identifier is a value: equality is by all of its components, by definition. */
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

    @Override
    public String toString() {
        return "ServiceOrderServiceId[serviceOrderId=%s, serviceId=%s]".formatted(serviceOrderId, serviceId);
    }
}
