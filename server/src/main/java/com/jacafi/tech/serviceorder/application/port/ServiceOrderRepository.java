package com.jacafi.tech.serviceorder.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public interface ServiceOrderRepository {
    ServiceOrder save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(UUID id);

    PageResult<ServiceOrder> findOperationalQueue(PageQuery query);
}
