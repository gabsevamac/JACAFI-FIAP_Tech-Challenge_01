package com.jacafi.tech.vehicle.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.FieldChange;
import com.jacafi.tech.vehicle.domain.AuditedOperation;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Corrects make, model and model year of a registered vehicle — the {@code UpdateVehicle} command.
 *
 * <p>A removed vehicle is not found here, rather than found and rejected: to any caller, a removed
 * vehicle and a vehicle that never existed are the same thing.
 *
 * <p>Writes to both trails, which record different things. {@code VehicleAuditTrail} records that
 * an update happened, by whom; {@code AuditTrailPort} records which fields moved and between which
 * values. Neither derives from the other.
 *
 * <p>The field comparison lives here and not in a JPA listener on purpose. A listener sees a value
 * change and cannot say why — and the reason is the part that matters, because correcting a typo
 * and recording a genuine change are the same two column values with opposite meanings. That is
 * hot spot HS9, and only the use case is in a position to answer it.
 */
@Service
public class UpdateVehicleUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateVehicleUseCase.class);

    private static final String AGGREGATE_TYPE = "Vehicle";

    private final VehicleRepository repository;
    private final VehicleAuditTrail auditTrail;
    private final AuditTrailPort fieldTrail;
    private final Clock clock;

    public UpdateVehicleUseCase(
            VehicleRepository repository, VehicleAuditTrail auditTrail, AuditTrailPort fieldTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.fieldTrail = fieldTrail;
        this.clock = clock;
    }

    @Transactional
    public Vehicle update(UpdateVehicleCommand command) {
        Vehicle vehicle = repository
                .findActiveById(command.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));

        // Lidos antes da mutacao: depois dela o valor anterior deixou de existir em qualquer lugar.
        String previousMake = vehicle.getMake();
        String previousModel = vehicle.getModel();
        int previousModelYear = vehicle.getModelYear();

        vehicle.update(command.make(), command.model(), command.modelYear(), clock);

        repository.save(vehicle);

        Instant changedAt = clock.instant();
        recordChange(vehicle, "make", previousMake, vehicle.getMake(), command, changedAt);
        recordChange(vehicle, "model", previousModel, vehicle.getModel(), command, changedAt);
        recordChange(
                vehicle,
                "modelYear",
                String.valueOf(previousModelYear),
                String.valueOf(vehicle.getModelYear()),
                command,
                changedAt);
        auditTrail.append(
                new VehicleAuditEntry(vehicle.getId(), AuditedOperation.UPDATED, command.actor(), clock.instant()));

        log.info(
                "Vehicle updated: id={} licensePlate={}",
                vehicle.getId(),
                vehicle.getLicensePlate().map(LicensePlate::masked).orElse("***"));
        return vehicle;
    }

    /**
     * Appends one entry, or none when the field did not actually move.
     *
     * <p>A request restating a field's current value is normal — a form posts every field whether
     * or not the user touched it. Recording those would fill the trail with entries whose old and
     * new values are equal, and the noise is not harmless: it makes "when did this last change"
     * unanswerable by reading the most recent row.
     */
    private void recordChange(
            Vehicle vehicle, String field, String oldValue, String newValue, UpdateVehicleCommand command, Instant at) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        fieldTrail.record(
                new FieldChange(AGGREGATE_TYPE, vehicle.getId(), field, oldValue, newValue, null, at, command.actor()));
    }
}
