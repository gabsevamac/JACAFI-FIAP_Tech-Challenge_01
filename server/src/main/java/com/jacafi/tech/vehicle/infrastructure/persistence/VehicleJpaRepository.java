package com.jacafi.tech.vehicle.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository over the storage shape.
 *
 * <p>Every derived query carries {@code AndRemovedAtIsNull}: a removed vehicle keeps its row for
 * the service history and answers nothing. This interface is an implementation detail of the
 * adapters in this package — the application layer talks to the domain port instead.
 */
interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    Optional<VehicleJpaEntity> findByIdAndRemovedAtIsNull(UUID id);

    Optional<VehicleJpaEntity> findByLicensePlateAndRemovedAtIsNull(String licensePlate);

    boolean existsByLicensePlateAndRemovedAtIsNull(String licensePlate);

    Page<VehicleJpaEntity> findByCustomerIdAndRemovedAtIsNull(UUID customerId, Pageable pageable);
}
