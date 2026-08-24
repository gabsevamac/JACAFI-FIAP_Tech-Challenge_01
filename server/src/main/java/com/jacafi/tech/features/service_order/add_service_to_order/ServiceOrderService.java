package com.jacafi.tech.features.service_order.add_service_to_order;

import com.jacafi.tech.features.service.domain.Service;
import com.jacafi.tech.features.service_order.domain.ServiceOrder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
