package com.jacafi.tech.features.service_order.add_service_to_order;

import com.jacafi.tech.features.service.domain.Service;
import com.jacafi.tech.features.service_order.domain.ServiceOrder;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "service_orders_service")
public class ServiceOrderService {

    @EmbeddedId
    private ServiceOrderServiceId id;

    @Column(name = "price_at_sale", nullable = false)
    private BigDecimal priceAtSale;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceOrderId")
    @JoinColumn(name = "service_order_id", nullable = false, updatable = false)
    private ServiceOrder serviceOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    private Service service;

    /**
     * Required by JPA. Kept {@code protected} so the invariants in
     * {@link #create(ServiceOrder, Service, BigDecimal, int)} cannot be bypassed.
     */
    protected ServiceOrderService() {
    }

    public static ServiceOrderService create(ServiceOrder serviceOrder, Service service,
                                             BigDecimal priceAtSale,
                                             int quantity) {

        if (priceAtSale == null || priceAtSale.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("priceAtSale must be zero or positive");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }

        var item = new ServiceOrderService();
        item.id = new ServiceOrderServiceId(serviceOrder.getId(), service.getId());
        item.serviceOrder = serviceOrder;
        item.service = service;
        item.priceAtSale = priceAtSale;
        item.quantity = quantity;
        return item;
    }

    public ServiceOrderServiceId getId() {
        return id;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public int getQuantity() {
        return quantity;
    }

    public ServiceOrder getServiceOrder() {
        return serviceOrder;
    }

    public Service getService() {
        return service;
    }

    /** Identity-based equality, the identity here being the composite {@link ServiceOrderServiceId}. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceOrderService other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "ServiceOrderService[id=%s, quantity=%d]".formatted(id, quantity);
    }
}
