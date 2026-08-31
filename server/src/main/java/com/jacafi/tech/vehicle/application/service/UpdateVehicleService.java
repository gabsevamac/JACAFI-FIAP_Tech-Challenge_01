package com.jacafi.tech.vehicle.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.VehicleNotFoundException;

public class UpdateVehicleService {

    private final VehicleRepositoryPort vehicles;
    private final AuditTrailPort auditTrail;
    private final VehicleAccessPolicy access;
    private final Clock clock;

    public UpdateVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        this.vehicles = vehicles;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public Vehicle update(UUID vehicleId, String make, String model, int modelYear) {
        access.requireOperationalAccess();
        Vehicle vehicle = vehicles.findActiveById(vehicleId).orElseThrow(VehicleNotFoundException::new);
        vehicle.changeDetails(make, model, modelYear, clock);
        String actor = access.currentActor();
        Vehicle saved = vehicles.save(vehicle, actor);
        auditTrail.record(new AuditEvent("Vehicle", saved.id(), "UPDATED", actor, clock.instant()));
        return saved;
    }
}
