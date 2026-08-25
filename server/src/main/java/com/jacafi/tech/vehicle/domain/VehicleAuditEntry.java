package com.jacafi.tech.vehicle.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One line of the audit trail: who did what to which vehicle, and when.
 *
 * <p>Required by Art. 37 of the LGPD, which obliges the controller to keep a record of the
 * processing operations it carries out.
 *
 * <p>The entry references the vehicle by identifier and never carries its plate. An audit trail
 * that reproduced the personal data it exists to watch over would defeat its own purpose — and
 * would keep that data alive after the removal that was supposed to erase it.
 *
 * @param vehicleId  the vehicle the operation acted upon
 * @param operation  what happened
 * @param actor      who did it, as identified by the subject of the JWT
 * @param occurredAt when it happened
 */
public record VehicleAuditEntry(UUID vehicleId, AuditedOperation operation, String actor, Instant occurredAt) {

    public VehicleAuditEntry {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
    }
}
