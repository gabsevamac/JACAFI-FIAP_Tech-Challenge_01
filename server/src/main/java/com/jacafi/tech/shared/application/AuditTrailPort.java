package com.jacafi.tech.shared.application;

public interface AuditTrailPort {

    void record(AuditEvent event);
}
