package com.jacafi.tech.vehicle.application.service;

import java.util.UUID;

import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.VehicleNotFoundException;

public final class FindVehicleService {

    private final VehicleRepositoryPort vehicles;
    private final VehicleAccessPolicy access;

    public FindVehicleService(VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        this.vehicles = vehicles;
        this.access = access;
    }

    public Vehicle findById(UUID vehicleId) {
        access.requireOperationalAccess();
        return vehicles.findActiveById(vehicleId).orElseThrow(VehicleNotFoundException::new);
    }

    public Vehicle findByLicensePlate(String licensePlate) {
        access.requireOperationalAccess();
        return vehicles.findActiveByLicensePlate(new LicensePlate(licensePlate))
                .orElseThrow(VehicleNotFoundException::new);
    }
}
