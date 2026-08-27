package com.jacafi.tech.inventory.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.Stock;

/**
 * Writes the audit trail to its own table, inside the transaction of the operation being audited.
 *
 * <p>Sharing the transaction is deliberate, and here it is more than tidiness: this trail is the
 * stock ledger. A withdrawal committed without its entry would leave a balance that dropped for
 * no recorded reason, and an entry for a withdrawal that rolled back would claim stock left the
 * shelf when it did not. Either one is worse than a failure.
 */
@Component
public class JpaInventoryAuditTrail implements InventoryAuditTrail {

    private final InventoryAuditJpaRepository repository;

    public JpaInventoryAuditTrail(InventoryAuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(InventoryAuditEntry entry) {
        repository.save(new InventoryAuditEntryJpaEntity(
                entry.inventoryItemId(),
                entry.operation(),
                entry.serviceOrderId(),
                entry.optionalQuantity().map(Stock::value).orElse(null),
                entry.actor(),
                entry.occurredAt()));
    }
}
