package com.jacafi.tech.servicecatalog.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "service_catalog_items")
class ServiceCatalogItemJpaEntity extends AuditableJpaEntity {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ServiceCatalogItemJpaEntity() {}

    ServiceCatalogItemJpaEntity(UUID id, String name, String description, BigDecimal basePrice, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.active = active;
    }

    void apply(ServiceCatalogItem item) {
        name = item.name();
        description = item.description();
        basePrice = item.basePrice();
        active = item.active();
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String description() {
        return description;
    }

    BigDecimal basePrice() {
        return basePrice;
    }

    boolean active() {
        return active;
    }
}
