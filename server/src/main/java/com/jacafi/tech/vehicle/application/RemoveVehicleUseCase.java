package com.jacafi.tech.vehicle.application;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.vehicle.domain.AuditedOperation;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Removes a vehicle from the active registry — the {@code RemoveVehicle} command.
 *
 * <p>The row is not deleted. The aggregate erases its plate, the persistence layer replaces the
 * stored value with an irreversible token, and the record survives with its service history: kept
 * as a legal obligation and for warranty (LGPD Art. 16 I), while the data subject's right to
 * erasure is satisfied by the plate being gone (Art. 18 VI).
 *
 * <p>See HS11 in the Event Storming board: this single operation currently serves two acts with
 * different legal bases — the workshop tidying up its registry, and a data subject exercising a
 * right. The audit trail records that the removal happened, but not under which basis.
 *
 * <p>Two removals in a row give 404 on the second, not a conflict: after the first, there is no
 * active vehicle left to find.
 */
@Service
public class RemoveVehicleUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemoveVehicleUseCase.class);

    private final VehicleRepository repository;
    private final VehicleAuditTrail auditTrail;
    private final Clock clock;

    public RemoveVehicleUseCase(VehicleRepository repository, VehicleAuditTrail auditTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public void remove(UUID vehicleId, String actor) {
        Vehicle vehicle =
                repository.findActiveById(vehicleId).orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        // Read the masked plate before the aggregate erases it, so the log line can still be
        // correlated with the registration that created the vehicle.
        String maskedPlate = vehicle.getLicensePlate().map(LicensePlate::masked).orElse("***");

        vehicle.remove(clock);

        repository.save(vehicle);
        auditTrail.append(new VehicleAuditEntry(vehicle.getId(), AuditedOperation.REMOVED, actor, clock.instant()));

        log.info(
                "Vehicle removed and license plate erased: id={} previousLicensePlate={}",
                vehicle.getId(),
                maskedPlate);
    }
}
