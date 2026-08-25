package com.jacafi.tech.vehicle.api.dto;

import com.jacafi.tech.shared.lgpd.PersonalData;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Vehicle as the API exposes it. The aggregate never leaves the application through the wire.
 *
 * <p>The plate appears in full here, and that is intentional: the requirement is that it never
 * reaches a <em>log</em>, and an authenticated caller asking for a vehicle needs to see which one
 * it is. {@code toString} is still masked, because this object does reach logs.
 */
public record VehicleResponse(
        UUID id,
        @PersonalData("LGPD Art. 5 I — returned in full to an authenticated caller, never logged")
        @Schema(example = "ABC1D23")
        String licensePlate,
        String make,
        String model,
        int modelYear,
        UUID customerId,
        Instant registeredAt,
        Instant updatedAt) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(),
                vehicle.getLicensePlate().map(LicensePlate::value).orElse(null),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getModelYear(),
                vehicle.getCustomerId(),
                vehicle.getRegisteredAt(),
                vehicle.getUpdatedAt());
    }

    @Override
    public String toString() {
        return "VehicleResponse[id=%s, licensePlate=%s]".formatted(id, LicensePlate.mask(licensePlate));
    }
}
