package com.jacafi.tech.service_order.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_orders_service")
public class LaunchedServiceJpaEntity {

    @EmbeddedId
    private LaunchedServiceIdJpaEntity id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceOrderId")
    @JoinColumn(name = "service_order_id", nullable = false, updatable = false)
    private ServiceOrderJpaEntity serviceOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private ServiceJpaEntity service;

    @Column(name = "price_at_sale", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceAtSale;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected LaunchedServiceJpaEntity() {}

    public LaunchedServiceJpaEntity(
            ServiceOrderJpaEntity serviceOrder, ServiceJpaEntity service, BigDecimal priceAtSale, int quantity) {
        this.id = new LaunchedServiceIdJpaEntity(serviceOrder.getId(), service.getId());
        this.serviceOrder = serviceOrder;
        this.service = service;
        this.priceAtSale = priceAtSale;
        this.quantity = quantity;
    }

    public void applyState(BigDecimal priceAtSale, int quantity) {
        this.priceAtSale = priceAtSale;
        this.quantity = quantity;
    }

    public LaunchedServiceIdJpaEntity getId() {
        return id;
    }

    public ServiceOrderJpaEntity getServiceOrder() {
        return serviceOrder;
    }

    public ServiceJpaEntity getService() {
        return service;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaunchedServiceJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
