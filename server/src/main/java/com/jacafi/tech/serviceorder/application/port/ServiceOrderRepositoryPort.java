package com.jacafi.tech.serviceorder.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;

public interface ServiceOrderRepositoryPort {
    ServiceOrder save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(UUID id);
}
