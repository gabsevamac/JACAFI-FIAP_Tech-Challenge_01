package com.jacafi.tech.vehicle.application;

import java.util.UUID;

/**
 * Input for correcting the descriptive attributes of a vehicle.
 *
 * <p>No license plate: the plate is the vehicle's business identity and is immutable after
 * registration. Its absence from this record is the rule being enforced by the type system rather
 * than by a check.
 *
 * @param actor who is performing the operation, taken from the JWT subject by the api layer
 */
public record UpdateVehicleCommand(UUID vehicleId,
                                   String make,
                                   String model,
                                   int modelYear,
                                   String actor) {
}
