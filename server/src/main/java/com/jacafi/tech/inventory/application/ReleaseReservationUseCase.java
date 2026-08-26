package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Gives held units back — the {@code ReleaseReservation} command, driven by
 * {@code WhenEstimateRejected} and by {@code WhenApprovalDeadlineExpires}.
 *
 * <p>Both policies end at the same place because the outcome is the same: the order will not be
 * executed, so the material it was holding belongs to whoever needs it next. The reason they
 * differ — a customer said no, or a customer said nothing for long enough — is recorded on the
 * order's side, which is where that distinction has consequences.
 *
 * <p>HS2 on the board is still open: nobody has said how long an estimate waits before it expires.
 * Until someone does, no reservation ever expires on its own, and stock stays held for as long as
 * the estimate does.
 */
@Service
public class ReleaseReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseReservationUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public ReleaseReservationUseCase(InventoryItemRepository repository,
                                     InventoryAuditTrail auditTrail,
                                     Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public Reservation release(UUID inventoryItemId, UUID serviceOrderId, String actor) {
        InventoryItem item = repository.findActiveByIdForUpdate(inventoryItemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(inventoryItemId));

        Reservation released = item.releaseReservation(serviceOrderId, clock);

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.movement(item.getId(), AuditedOperation.RELEASED,
                serviceOrderId, released.quantity(), actor, clock.instant()));

        log.info("Reservation released: id={} name={} serviceOrderId={} released={} available={}",
                item.getId(), item.getName(), serviceOrderId, released.quantity(),
                item.quantityAvailable());
        return released;
    }
}
