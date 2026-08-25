package com.jacafi.tech.vehicle.domain;

/**
 * Port for appending to the audit trail.
 *
 * <p>Append-only by design: there is no method to read, amend or delete an entry. A trail whose
 * entries can be rewritten is not evidence of anything.
 */
public interface VehicleAuditTrail {

    void append(VehicleAuditEntry entry);
}
