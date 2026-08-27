package com.jacafi.tech.vehicle.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

@Entity
@Table(name = "vehicles")
class VehicleJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "license_plate", nullable = false, updatable = false, length = 64)
    private String licensePlate;

    @Column(name = "make", nullable = false, length = 60)
    private String make;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    protected VehicleJpaEntity() {}

    VehicleJpaEntity(UUID id, String licensePlate, String make, String model, int modelYear, UUID customerId) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.customerId = customerId;
    }

    void apply(Vehicle vehicle, String actor) {
        make = vehicle.make();
        model = vehicle.model();
        modelYear = vehicle.modelYear();
        if (!vehicle.active() && !isDeleted()) {
            markDeleted(vehicle.removedAt().orElseThrow(), requireActor(actor));
        }
    }

    UUID id() {
        return id;
    }

    String licensePlate() {
        return licensePlate;
    }

    String make() {
        return make;
    }

    String model() {
        return model;
    }

    int modelYear() {
        return modelYear;
    }

    UUID customerId() {
        return customerId;
    }

    Instant removedAt() {
        return getDeletedAt().orElse(null);
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        return actor;
    }
}
