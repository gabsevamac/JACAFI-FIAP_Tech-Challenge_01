package com.jacafi.tech.inventory.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class ReplenishInventoryStockService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAuditLedgerPort ledger;
    private final AuditTrailPort auditTrail;
    private final InventoryAccessPolicy access;
    private final Clock clock;

    public ReplenishInventoryStockService(
            InventoryItemRepositoryPort items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort auditTrail,
            InventoryAccessPolicy access,
            Clock clock) {
        this.items = items;
        this.ledger = ledger;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public InventoryItem replenish(UUID id, int quantity) {
        access.requireEmployee();
        var item = items.findActiveByIdForUpdate(id).orElseThrow(InventoryItemNotFoundException::new);
        Stock added = Stock.of(quantity);
        item.replenish(added, clock);
        String actor = access.currentActor();
        InventoryItem saved = items.save(item, actor);
        var at = clock.instant();
        ledger.append(InventoryAuditEntry.replenishment(id, added, actor, at));
        auditTrail.record(new AuditEvent("InventoryItem", id, "REPLENISHED", actor, at));
        return saved;
    }
}
