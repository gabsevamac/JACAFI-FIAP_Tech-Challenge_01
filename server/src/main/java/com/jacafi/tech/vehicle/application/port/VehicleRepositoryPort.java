package com.jacafi.tech.vehicle.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

public interface VehicleRepositoryPort {

    Vehicle save(Vehicle vehicle, String actor);

    boolean existsActiveByLicensePlate(LicensePlate licensePlate);

    Optional<Vehicle> findActiveById(UUID vehicleId);

    Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate);

    PageResult<Vehicle> findActiveByCustomerId(UUID customerId, PageQuery query);
}
