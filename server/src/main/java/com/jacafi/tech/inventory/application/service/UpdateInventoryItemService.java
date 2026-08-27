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
import com.jacafi.tech.inventory.domain.exception.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class UpdateInventoryItemService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAuditLedgerPort ledger;
    private final AuditTrailPort auditTrail;
    private final InventoryAccessPolicy access;
    private final Clock clock;

    public UpdateInventoryItemService(
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
    public InventoryItem update(UUID id, String name, BigDecimal unitPrice) {
        access.requireOperationalAccess();
        InventoryItem item = items.findActiveByIdForUpdate(id).orElseThrow(InventoryItemNotFoundException::new);
        item.update(name, unitPrice, clock);
        if (items.existsActiveWithNameExcluding(item.name(), id)) throw new DuplicateMaterialException();
        String actor = access.currentActor();
        InventoryItem saved = items.save(item, actor);
        var at = clock.instant();
        ledger.append(InventoryAuditEntry.action(saved.id(), AuditedOperation.UPDATED, actor, at));
        auditTrail.record(new AuditEvent("InventoryItem", saved.id(), "UPDATED", actor, at));
        return saved;
    }
}
