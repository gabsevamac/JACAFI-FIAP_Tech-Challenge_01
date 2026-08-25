package com.jacafi.tech.vehicle.application;

import com.jacafi.tech.vehicle.domain.AuditedOperation;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;
import com.jacafi.tech.vehicle.domain.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Corrects make, model and model year of a registered vehicle — the {@code UpdateVehicle} command.
 *
 * <p>A removed vehicle is not found here, rather than found and rejected: to any caller, a removed
 * vehicle and a vehicle that never existed are the same thing.
 */
@Service
public class UpdateVehicleUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateVehicleUseCase.class);

    private final VehicleRepository repository;
    private final VehicleAuditTrail auditTrail;
    private final Clock clock;

    public UpdateVehicleUseCase(VehicleRepository repository, VehicleAuditTrail auditTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public Vehicle update(UpdateVehicleCommand command) {
        Vehicle vehicle = repository.findActiveById(command.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));

        vehicle.update(command.make(), command.model(), command.modelYear(), clock);

        repository.save(vehicle);
        auditTrail.append(new VehicleAuditEntry(vehicle.getId(), AuditedOperation.UPDATED,
                command.actor(), clock.instant()));

        log.info("Vehicle updated: id={} licensePlate={}", vehicle.getId(),
                vehicle.getLicensePlate().map(LicensePlate::masked).orElse("***"));
        return vehicle;
    }
}
