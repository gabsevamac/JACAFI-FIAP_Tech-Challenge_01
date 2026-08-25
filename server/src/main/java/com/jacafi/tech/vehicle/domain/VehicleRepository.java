package com.jacafi.tech.vehicle.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the aggregate is stored and retrieved.
 *
 * <p>Scope is deliberately the aggregate's own lifecycle: persist one vehicle, load one vehicle,
 * answer the uniqueness question the registration rule depends on. Reads that exist to fill a
 * screen — listing, paging, sorting, filtering — are a delivery concern, not a domain rule, and
 * live behind a read port in the application layer. No invariant of this aggregate knows what
 * page two of size twenty is.
 *
 * <p>Every lookup here is restricted to active vehicles. A removed vehicle keeps its row, for the
 * service history that has to be preserved, but it answers no query and does not hold its former
 * plate against a new registration.
 */
public interface VehicleRepository {

    /** Inserts or updates the aggregate. */
    void save(Vehicle vehicle);

    Optional<Vehicle> findActiveById(UUID id);

    Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate);

    /**
     * Whether an active vehicle already carries this plate. Separate from the lookup above
     * because the registration rule only needs the answer, and loading the conflicting vehicle
     * would put a plate that belongs to someone else in the caller's hands for no reason.
     */
    boolean existsActiveWithLicensePlate(LicensePlate licensePlate);
}
