package com.jacafi.tech.vehicle.application;

import java.util.UUID;

/**
 * Input for registering a vehicle.
 *
 * <p>Carries primitives, not value objects: it crosses the boundary inward from a caller that has
 * only strings and numbers, and turning those into a {@code LicensePlate} or a {@code CustomerId}
 * is the use case's job — which is also where the resulting validation failure belongs.
 *
 * @param actor who is performing the operation, taken from the JWT subject by the api layer
 */
public record RegisterVehicleCommand(String licensePlate,
                                     String make,
                                     String model,
                                     int modelYear,
                                     UUID customerId,
                                     String actor) {

    /** Never prints the plate: a command is as loggable as anything else, so it must be safe. */
    @Override
    public String toString() {
        return "RegisterVehicleCommand[customerId=%s, modelYear=%d, actor=%s]"
                .formatted(customerId, modelYear, actor);
    }
}
