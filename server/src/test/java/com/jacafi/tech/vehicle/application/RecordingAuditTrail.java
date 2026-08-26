package com.jacafi.tech.vehicle.application;

import java.util.ArrayList;
import java.util.List;

import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;

/** Collects the audit entries a use case appends, so a test can assert what was recorded. */
class RecordingAuditTrail implements VehicleAuditTrail {

    private final List<VehicleAuditEntry> entries = new ArrayList<>();

    @Override
    public void append(VehicleAuditEntry entry) {
        entries.add(entry);
    }

    List<VehicleAuditEntry> entries() {
        return List.copyOf(entries);
    }

    VehicleAuditEntry only() {
        if (entries.size() != 1) {
            throw new AssertionError("Expected exactly one audit entry but found " + entries.size());
        }
        return entries.getFirst();
    }
}
