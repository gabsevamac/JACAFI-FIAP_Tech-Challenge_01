package com.jacafi.tech.serviceorder.application.service;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public class ListOperationalServiceOrdersService {
    private final ServiceOrderRepository orders;
    private final ServiceOrderAccessPolicy access;

    public ListOperationalServiceOrdersService(ServiceOrderRepository orders, ServiceOrderAccessPolicy access) {
        this.orders = orders;
        this.access = access;
    }

    public PageResult<ServiceOrder> list(PageQuery query) {
        access.requireOperationalAccess();
        return orders.findOperationalQueue(query);
    }
}
