package com.jacafi.tech.inventory.application.port;

import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;

public interface InventoryAuditLedgerPort {
    void append(InventoryAuditEntry entry);
}
