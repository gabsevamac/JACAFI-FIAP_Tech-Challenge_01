package com.jacafi.tech.vehicle.application;

import com.jacafi.tech.vehicle.domain.AuditedOperation;
import com.jacafi.tech.vehicle.domain.DuplicateLicensePlateException;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;
import com.jacafi.tech.vehicle.domain.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Registers a vehicle for a customer — the {@code RegisterVehicle} command of the Event Storming
 * board.
 *
 * <p>This is where plate uniqueness is enforced. The aggregate cannot do it: uniqueness spans the
 * whole collection of vehicles, and an aggregate only guards its own state. Doing the check here
 * is also what turns a duplicate into a business rule violation with a name, instead of a
 * constraint violation surfacing from the driver.
 *
 * <p>The unique index in the database still matters, for two concurrent registrations of the same
 * plate that both pass this check. That case belongs to the persistence layer, which translates
 * the violation back into {@link DuplicateLicensePlateException}.
 */
@Service
public class RegisterVehicleUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterVehicleUseCase.class);

    private final VehicleRepository repository;
    private final VehicleAuditTrail auditTrail;
    private final Clock clock;

    public RegisterVehicleUseCase(VehicleRepository repository, VehicleAuditTrail auditTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public Vehicle register(RegisterVehicleCommand command) {
        LicensePlate licensePlate = new LicensePlate(command.licensePlate());

        if (repository.existsActiveWithLicensePlate(licensePlate)) {
            // No plate in the message, not even the one that caused the conflict.
            throw new DuplicateLicensePlateException("A vehicle with this license plate is already registered.");
        }

        Vehicle vehicle = Vehicle.builder()
                .id(UUID.randomUUID())
                .licensePlate(licensePlate)
                .make(command.make())
                .model(command.model())
                .modelYear(command.modelYear())
                .customerId(command.customerId())
                .register(clock);

        repository.save(vehicle);
        auditTrail.append(new VehicleAuditEntry(vehicle.getId(), AuditedOperation.REGISTERED,
                command.actor(), clock.instant()));

        log.info("Vehicle registered: id={} licensePlate={}", vehicle.getId(), licensePlate.masked());
        return vehicle;
    }
}
