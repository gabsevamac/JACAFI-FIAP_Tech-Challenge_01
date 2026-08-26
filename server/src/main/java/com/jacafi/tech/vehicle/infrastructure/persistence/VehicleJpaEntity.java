package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * Storage shape of a vehicle. Deliberately separate from the aggregate: {@code domain/} may not
 * import {@code jakarta.persistence}, so the ORM mapping lives here and a mapper moves state
 * across. The boilerplate is the price of that boundary.
 *
 * <p>A JPA entity cannot be a record — the specification requires a no-args constructor and
 * non-final fields — so this is a plain class. It has no setters either: the mapper builds a full
 * instance and the adapter merges it, which keeps "partially updated row" from being a
 * representable state.
 */
@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Holds the plate while the vehicle is active and an irreversible token afterwards, which is
     * why it is wider than the seven characters a plate needs.
     */
    @PersonalData("LGPD Art. 5 I — cleared on removal, replaced by an irreversible token")
    @Column(name = "license_plate", nullable = false, length = 64)
    private String licensePlate;

    @Column(name = "make", nullable = false, length = 60)
    private String make;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Null while the vehicle is active. Its presence is what removes a row from every query. */
    @Column(name = "removed_at")
    private Instant removedAt;

    /**
     * Required by JPA, which instantiates entities reflectively before populating their state.
     * Kept {@code protected} so only Hibernate and the mapper in this package can reach it.
     */
    protected VehicleJpaEntity() {}

    VehicleJpaEntity(
            UUID id,
            String licensePlate,
            String make,
            String model,
            int modelYear,
            UUID customerId,
            Instant registeredAt,
            Instant updatedAt,
            Instant removedAt) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.customerId = customerId;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.removedAt = removedAt;
    }

    UUID getId() {
        return id;
    }

    String getLicensePlate() {
        return licensePlate;
    }

    String getMake() {
        return make;
    }

    String getModel() {
        return model;
    }

    int getModelYear() {
        return modelYear;
    }

    UUID getCustomerId() {
        return customerId;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getRemovedAt() {
        return removedAt;
    }

    /** Identity-based equality: field-based equality breaks for managed entities. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    /** Masked, for the same reason the aggregate's is: this object reaches logs too. */
    @Override
    public String toString() {
        return "VehicleJpaEntity[id=%s, licensePlate=%s, removed=%s]"
                .formatted(id, com.jacafi.tech.vehicle.domain.LicensePlate.mask(licensePlate), removedAt != null);
    }
}
