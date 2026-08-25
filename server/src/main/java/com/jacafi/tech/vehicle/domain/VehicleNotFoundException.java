package com.jacafi.tech.vehicle.domain;

import java.util.UUID;

/**
 * No active vehicle exists for the given identifier.
 *
 * <p>A removed vehicle is indistinguishable from a non-existent one to any caller: its record
 * survives for the service history required by Art. 16 I, but it answers no query.
 */
public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(UUID vehicleId) {
        // A vehicle identifier is a surrogate key, not personal data, so it may appear here.
        super("No active vehicle found for id " + vehicleId);
    }

    /** For the lookup by plate, where the searched value may not be echoed back. */
    public VehicleNotFoundException() {
        super("No active vehicle found for the given license plate");
    }
}
