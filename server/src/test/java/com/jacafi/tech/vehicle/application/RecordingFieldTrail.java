package com.jacafi.tech.vehicle.application;

import java.util.ArrayList;
import java.util.List;

import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.FieldChange;

/** Collects the field changes a use case records, so a unit test can assert on them without JPA. */
class RecordingFieldTrail implements AuditTrailPort {

    private final List<FieldChange> changes = new ArrayList<>();

    @Override
    public void record(FieldChange change) {
        changes.add(change);
    }

    List<FieldChange> changes() {
        return List.copyOf(changes);
    }

    List<String> fieldNames() {
        return changes.stream().map(FieldChange::fieldName).toList();
    }
}
