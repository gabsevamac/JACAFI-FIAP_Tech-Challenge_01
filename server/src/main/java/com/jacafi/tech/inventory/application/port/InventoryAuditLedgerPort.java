package com.jacafi.tech.inventory.application.port;

import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;

/** Port for the append-only inventory ledger. */
public interface InventoryAuditLedgerPort {
    void append(InventoryAuditEntry entry);
}
