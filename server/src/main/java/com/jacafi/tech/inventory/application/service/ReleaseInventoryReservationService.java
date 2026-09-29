package com.jacafi.tech.inventory.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.AuditedOperation;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.Reservation;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class ReleaseInventoryReservationService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAuditLedgerPort ledger;
    private final AuditTrailPort auditTrail;
    private final InventoryAccessPolicy access;
    private final Clock clock;

    public ReleaseInventoryReservationService(
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
    public Reservation release(UUID id, UUID serviceOrderId) {
        access.requireEmployee();
        var item = items.findActiveByIdForUpdate(id).orElseThrow(InventoryItemNotFoundException::new);
        Reservation released = item.releaseReservation(serviceOrderId, clock);
        String actor = access.currentActor();
        items.save(item, actor);
        var at = clock.instant();
        ledger.append(InventoryAuditEntry.movement(
                id, AuditedOperation.RELEASED, serviceOrderId, released.quantity(), actor, at));
        auditTrail.record(new AuditEvent("InventoryItem", id, "RELEASED", actor, at));
        return released;
    }
}
