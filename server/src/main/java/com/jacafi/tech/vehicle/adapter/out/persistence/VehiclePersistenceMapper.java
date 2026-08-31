package com.jacafi.tech.vehicle.adapter.out.persistence;

import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

final class VehiclePersistenceMapper {

    private VehiclePersistenceMapper() {}

    static VehicleJpaEntity toJpa(Vehicle vehicle) {
        return new VehicleJpaEntity(
                vehicle.id(),
                vehicle.licensePlate().value(),
                vehicle.make(),
                vehicle.model(),
                vehicle.modelYear(),
                vehicle.customerId());
    }

    static Vehicle toDomain(VehicleJpaEntity entity) {
        return Vehicle.restore(
                entity.id(),
                new LicensePlate(entity.licensePlate()),
                entity.make(),
                entity.model(),
                entity.modelYear(),
                entity.customerId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.removedAt());
    }
}
