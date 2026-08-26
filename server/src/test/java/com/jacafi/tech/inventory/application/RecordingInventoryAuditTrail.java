package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;

import java.util.ArrayList;
import java.util.List;

/** Collects the audit entries a use case appends, so a test can assert what was recorded. */
class RecordingInventoryAuditTrail implements InventoryAuditTrail {

    private final List<InventoryAuditEntry> entries = new ArrayList<>();

    @Override
    public void append(InventoryAuditEntry entry) {
        entries.add(entry);
    }

    List<InventoryAuditEntry> entries() {
        return List.copyOf(entries);
    }

    InventoryAuditEntry only() {
        if (entries.size() != 1) {
            throw new AssertionError("Expected exactly one audit entry but found " + entries.size());
        }
        return entries.getFirst();
    }

    InventoryAuditEntry last() {
        if (entries.isEmpty()) {
            throw new AssertionError("Expected at least one audit entry but found none");
        }
        return entries.getLast();
    }
}
