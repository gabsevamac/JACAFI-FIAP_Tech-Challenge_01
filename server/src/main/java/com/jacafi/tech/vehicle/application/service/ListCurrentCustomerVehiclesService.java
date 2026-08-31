package com.jacafi.tech.vehicle.application.service;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

public final class ListCurrentCustomerVehiclesService {

    private final VehicleRepositoryPort vehicles;
    private final VehicleAccessPolicy access;

    public ListCurrentCustomerVehiclesService(VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        this.vehicles = vehicles;
        this.access = access;
    }

    public PageResult<Vehicle> list(PageQuery query) {
        return vehicles.findActiveByCustomerId(access.currentCustomerId(), query);
    }
}
