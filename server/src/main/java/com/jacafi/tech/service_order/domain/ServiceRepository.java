package com.jacafi.tech.service_order.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the {@link Service} catalog aggregate is stored and retrieved.
 */
public interface ServiceRepository {

    void save(Service service);

    Optional<Service> findById(UUID id);

    List<Service> findAll();

    void deleteById(UUID id);
}
