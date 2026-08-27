package com.jacafi.tech.vehicle.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.VehicleNotFoundException;

public class RemoveVehicleService {

    private final VehicleRepositoryPort vehicles;
    private final AuditTrailPort auditTrail;
    private final VehicleAccessPolicy access;
    private final Clock clock;

    public RemoveVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        this.vehicles = vehicles;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public void remove(UUID vehicleId) {
        access.requireOperationalAccess();
        Vehicle vehicle = vehicles.findActiveById(vehicleId).orElseThrow(VehicleNotFoundException::new);
        String actor = access.currentActor();
        vehicle.remove(clock.instant());
        Vehicle saved = vehicles.save(vehicle, actor);
        auditTrail.record(new AuditEvent("Vehicle", saved.id(), "REMOVED", actor, clock.instant()));
    }
}
