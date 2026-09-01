package com.jacafi.tech.serviceorder.application.service;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public class ListOperationalServiceOrdersService {
    private final ServiceOrderRepositoryPort orders;
    private final ServiceOrderAccessPolicy access;

    public ListOperationalServiceOrdersService(ServiceOrderRepositoryPort orders, ServiceOrderAccessPolicy access) {
        this.orders = orders;
        this.access = access;
    }

    public PageResult<ServiceOrder> list(PageQuery query) {
        access.requireEmployee();
        return orders.findOperationalQueue(query);
    }
}
