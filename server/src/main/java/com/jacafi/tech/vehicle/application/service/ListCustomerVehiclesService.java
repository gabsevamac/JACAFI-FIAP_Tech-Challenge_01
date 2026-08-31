package com.jacafi.tech.vehicle.application.service;

import java.util.UUID;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

public final class ListCustomerVehiclesService {

    private final VehicleRepositoryPort vehicles;
    private final VehicleAccessPolicy access;

    public ListCustomerVehiclesService(VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        this.vehicles = vehicles;
        this.access = access;
    }

    public PageResult<Vehicle> list(UUID customerId, PageQuery query) {
        access.requireOperationalAccess();
        return vehicles.findActiveByCustomerId(customerId, query);
    }
}
