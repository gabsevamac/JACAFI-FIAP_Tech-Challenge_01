package com.jacafi.tech.vehicle.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

public record VehicleResponse(
        UUID id,
        String licensePlate,
        String make,
        String model,
        int modelYear,
        UUID customerId,
        Instant registeredAt,
        Instant updatedAt) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.id(),
                vehicle.licensePlate().value(),
                vehicle.make(),
                vehicle.model(),
                vehicle.modelYear(),
                vehicle.customerId(),
                vehicle.registeredAt(),
                vehicle.updatedAt());
    }

    @Override
    public String toString() {
        return "VehicleResponse[id=%s, licensePlate=%s]".formatted(id, LicensePlate.mask(licensePlate));
    }
}
