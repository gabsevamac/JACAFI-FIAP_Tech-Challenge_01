package com.jacafi.tech.inventory.domain;

/**
 * Port for appending to the audit trail.
 *
 * <p>Append-only by design: there is no method to read, amend or delete an entry. A trail whose
 * entries can be rewritten is not evidence of anything — and here it is also the stock ledger, so
 * a rewritable entry would be a stock balance nobody could defend.
 */
public interface InventoryAuditTrail {

    void append(InventoryAuditEntry entry);
}
