package com.jacafi.tech.shared.application;

/** Port for appending successful business actions to the shared audit trail. */
public interface AuditTrailPort {

    void record(AuditEvent event);
}
