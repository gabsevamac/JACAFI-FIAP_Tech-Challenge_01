package com.jacafi.tech.inventory.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.AuditedOperation;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.inventory.domain.exception.DuplicateMaterialException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class RegisterInventoryItemService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAuditLedgerPort ledger;
    private final AuditTrailPort auditTrail;
    private final InventoryAccessPolicy access;
    private final Clock clock;

    public RegisterInventoryItemService(
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
    public InventoryItem register(String name, MaterialType type, BigDecimal unitPrice, int initialStock) {
        access.requireEmployee();
        InventoryItem item =
                InventoryItem.register(UUID.randomUUID(), name, type, unitPrice, Stock.of(initialStock), clock);
        if (items.existsActiveWithName(item.name())) throw new DuplicateMaterialException();
        String actor = access.currentActor();
        InventoryItem saved = items.save(item, actor);
        record(saved.id(), AuditedOperation.REGISTERED, null, null, actor);
        return saved;
    }

    private void record(UUID id, AuditedOperation operation, UUID serviceOrderId, Stock quantity, String actor) {
        var at = clock.instant();
        ledger.append(
                serviceOrderId == null && quantity == null
                        ? InventoryAuditEntry.action(id, operation, actor, at)
                        : serviceOrderId == null
                                ? InventoryAuditEntry.replenishment(id, quantity, actor, at)
                                : InventoryAuditEntry.movement(id, operation, serviceOrderId, quantity, actor, at));
        auditTrail.record(new AuditEvent("InventoryItem", id, operation.name(), actor, at));
    }
}
