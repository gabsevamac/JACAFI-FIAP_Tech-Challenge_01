package com.jacafi.tech.vehicle.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.DuplicateLicensePlateException;

public class RegisterVehicleService {

    private final VehicleRepositoryPort vehicles;
    private final AuditTrailPort auditTrail;
    private final VehicleAccessPolicy access;
    private final Clock clock;

    public RegisterVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        this.vehicles = vehicles;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public Vehicle register(String licensePlate, String make, String model, int modelYear, UUID customerId) {
        access.requireOperationalAccess();
        LicensePlate parsedPlate = new LicensePlate(licensePlate);
        if (vehicles.existsActiveByLicensePlate(parsedPlate)) {
            throw new DuplicateLicensePlateException();
        }
        String actor = access.currentActor();
        Vehicle saved = vehicles.save(
                Vehicle.register(UUID.randomUUID(), parsedPlate, make, model, modelYear, customerId, clock), actor);
        auditTrail.record(new AuditEvent("Vehicle", saved.id(), "REGISTERED", actor, clock.instant()));
        return saved;
    }
}
