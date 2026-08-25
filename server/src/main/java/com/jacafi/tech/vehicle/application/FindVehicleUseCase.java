package com.jacafi.tech.vehicle.application;

import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;
import com.jacafi.tech.vehicle.domain.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reads a vehicle — the {@code FindVehicle} command of the Event Storming board.
 *
 * <p>Two ways in, because the vehicle has two identities: the surrogate identifier used by other
 * slices to reference it, and the license plate, which is how the workshop actually recognizes a
 * vehicle that has been here before.
 *
 * <p>Absence is an exception rather than an empty {@code Optional} returned to the caller: every
 * one of these lookups exists to serve a request for one specific vehicle, and "not there" is the
 * same answer in all of them. Keeping the decision here means the api layer does not repeat it.
 *
 * <p>Paged listing lives in {@link ListCustomerVehiclesUseCase}, since it answers a different
 * question — everything this customer owns, rather than this vehicle.
 */
@Service
public class FindVehicleUseCase {

    private final VehicleRepository repository;

    public FindVehicleUseCase(VehicleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Vehicle byId(UUID vehicleId) {
        return repository.findActiveById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    /**
     * @param licensePlate as typed by the caller; normalized and validated on the way in, so
     *                     {@code abc-1234} finds the vehicle registered as {@code ABC1234}
     */
    @Transactional(readOnly = true)
    public Vehicle byLicensePlate(String licensePlate) {
        return repository.findActiveByLicensePlate(new LicensePlate(licensePlate))
                .orElseThrow(VehicleNotFoundException::new);
    }
}
