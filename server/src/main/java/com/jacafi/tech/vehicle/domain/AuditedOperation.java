package com.jacafi.tech.vehicle.domain;

/**
 * The write operations recorded in the audit trail.
 *
 * <p>Named in the past tense, after the domain events fixed in §9 of the dictionary
 * ({@code VehicleRegistered}, {@code VehicleUpdated}, {@code VehicleRemoved}): the trail records
 * what happened, not what was asked for.
 */
public enum AuditedOperation {
    REGISTERED,
    UPDATED,
    REMOVED
}
