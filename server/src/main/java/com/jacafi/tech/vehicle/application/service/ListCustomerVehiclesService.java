package com.jacafi.tech.vehicle.application.service;

import java.util.UUID;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.application.port.VehicleRepository;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

public final class ListCustomerVehiclesService {

    private final VehicleRepository vehicles;
    private final VehicleAccessPolicy access;

    public ListCustomerVehiclesService(VehicleRepository vehicles, VehicleAccessPolicy access) {
        this.vehicles = vehicles;
        this.access = access;
    }

    public PageResult<Vehicle> list(UUID customerId, PageQuery query) {
        access.requireEmployee();
        return vehicles.findActiveByCustomerId(customerId, query);
    }
}
