package com.jacafi.tech.service_order.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Storage shape of a catalog service.
 */
@Entity
@Table(name = "services")
public class ServiceJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "description", nullable = false, length = 45)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 38, scale = 2)
    private BigDecimal basePrice;

    protected ServiceJpaEntity() {}

    public ServiceJpaEntity(UUID id, String description, BigDecimal basePrice) {
        this.id = id;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void applyState(String description, BigDecimal basePrice) {
        this.description = description;
        this.basePrice = basePrice;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
