package com.jacafi.tech.serviceorder.application.service;

import java.util.UUID;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;

public class FindServiceOrderStatusService {
    private final ServiceOrderRepository orders;
    private final ServiceOrderAccessPolicy access;

    public FindServiceOrderStatusService(ServiceOrderRepository orders, ServiceOrderAccessPolicy access) {
        this.orders = orders;
        this.access = access;
    }

    public ServiceOrder find(UUID id) {
        ServiceOrder order = orders.findById(id).orElseThrow(ServiceOrderNotFoundException::new);
        access.requireReadAccess(order.customerId());
        return order;
    }
}
