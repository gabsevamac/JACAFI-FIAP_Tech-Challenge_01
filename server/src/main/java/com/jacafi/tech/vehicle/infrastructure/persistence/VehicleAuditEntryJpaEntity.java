package com.jacafi.tech.vehicle.infrastructure.persistence;

import com.jacafi.tech.vehicle.domain.AuditedOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Storage shape of one audit trail line (LGPD Art. 37).
 *
 * <p>Carries the vehicle identifier and never its plate. An audit trail that copied the personal
 * data it exists to watch over would keep that data alive past the removal meant to erase it.
 *
 * <p>Append-only: there is no setter, no update path and no delete method anywhere above it.
 */
@Entity
@Table(name = "vehicle_audit_entries")
public class VehicleAuditEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, updatable = false, length = 20)
    private AuditedOperation operation;

    @Column(name = "actor", nullable = false, updatable = false, length = 120)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Required by JPA. */
    protected VehicleAuditEntryJpaEntity() {
    }

    VehicleAuditEntryJpaEntity(UUID vehicleId, AuditedOperation operation, String actor, Instant occurredAt) {
        this.vehicleId = vehicleId;
        this.operation = operation;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    Long getId() {
        return id;
    }

    UUID getVehicleId() {
        return vehicleId;
    }

    AuditedOperation getOperation() {
        return operation;
    }

    String getActor() {
        return actor;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleAuditEntryJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "VehicleAuditEntryJpaEntity[id=%s, vehicleId=%s, operation=%s]"
                .formatted(id, vehicleId, operation);
    }
}
