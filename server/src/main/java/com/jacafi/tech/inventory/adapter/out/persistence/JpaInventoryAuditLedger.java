package com.jacafi.tech.inventory.adapter.out.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;

@Component
public class JpaInventoryAuditLedger implements InventoryAuditLedgerPort {
    private final InventoryAuditJpaRepository entries;

    public JpaInventoryAuditLedger(InventoryAuditJpaRepository entries) {
        this.entries = entries;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(InventoryAuditEntry entry) {
        entries.save(new InventoryAuditEntryJpaEntity(
                entry.inventoryItemId(),
                entry.operation(),
                entry.serviceOrderId(),
                entry.quantity() == null ? null : entry.quantity().value(),
                entry.actor(),
                entry.occurredAt()));
    }
}
