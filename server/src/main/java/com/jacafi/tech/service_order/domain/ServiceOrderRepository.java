package com.jacafi.tech.service_order.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the {@link ServiceOrder} aggregate is stored and retrieved.
 */
public interface ServiceOrderRepository {

    void save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(UUID id);

    List<ServiceOrder> findAll();
}
