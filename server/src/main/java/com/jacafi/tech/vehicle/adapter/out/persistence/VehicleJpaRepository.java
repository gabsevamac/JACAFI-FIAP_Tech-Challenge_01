package com.jacafi.tech.vehicle.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    Optional<VehicleJpaEntity> findByIdAndDeletedAtIsNull(UUID vehicleId);

    Optional<VehicleJpaEntity> findByLicensePlateAndDeletedAtIsNull(String licensePlate);

    boolean existsByLicensePlateAndDeletedAtIsNull(String licensePlate);

    Page<VehicleJpaEntity> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);
}
