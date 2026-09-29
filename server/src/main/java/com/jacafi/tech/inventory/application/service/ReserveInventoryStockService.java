package com.jacafi.tech.inventory.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.AuditedOperation;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class ReserveInventoryStockService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAuditLedgerPort ledger;
    private final AuditTrailPort auditTrail;
    private final InventoryAccessPolicy access;
    private final Clock clock;

    public ReserveInventoryStockService(
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
    public InventoryItem reserve(UUID id, UUID serviceOrderId, int quantity) {
        access.requireEmployee();
        var item = items.findActiveByIdForUpdate(id).orElseThrow(InventoryItemNotFoundException::new);
        Stock requested = Stock.of(quantity);
        item.reserve(serviceOrderId, requested, clock);
        String actor = access.currentActor();
        InventoryItem saved = items.save(item, actor);
        var at = clock.instant();
        ledger.append(
                InventoryAuditEntry.movement(id, AuditedOperation.RESERVED, serviceOrderId, requested, actor, at));
        auditTrail.record(new AuditEvent("InventoryItem", id, "RESERVED", actor, at));
        return saved;
    }
}
