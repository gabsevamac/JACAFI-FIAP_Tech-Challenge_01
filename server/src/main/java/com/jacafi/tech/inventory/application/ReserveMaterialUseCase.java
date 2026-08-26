package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Holds units for a service order — the {@code ReserveMaterial} command, which the board triggers
 * from the {@code WhenEstimateApproved} policy rather than from an actor.
 *
 * <p>Policy, never a direct call. The service order slice does not invoke this use case and does
 * not import this package; when the policy is wired, it will react to {@code EstimateApproved} and
 * come in through the same command as any other caller. That indirection is the aggregate boundary
 * doing its job — the board is explicit that consistency between {@code ServiceOrder} and
 * {@code Inventory} tolerates delay, and this is where the delay is allowed to live.
 *
 * <p>Until that policy exists, the endpoint is the way in, and the actor is whoever holds the JWT.
 * The reservation still names the order it belongs to, so nothing about the record changes when
 * the caller does.
 *
 * <p>Loaded under a lock: reserving is read-then-write on a shared count, and two callers
 * reserving the last unit at the same time would otherwise both succeed. See
 * {@code InventoryItemRepository#findActiveByIdForUpdate}.
 */
@Service
public class ReserveMaterialUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveMaterialUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public ReserveMaterialUseCase(InventoryItemRepository repository,
                                  InventoryAuditTrail auditTrail,
                                  Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    /** @return the item as it now stands, so the caller can see what is left available */
    @Transactional
    public InventoryItem reserve(ReserveMaterialCommand command) {
        InventoryItem item = repository.findActiveByIdForUpdate(command.inventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException(command.inventoryItemId()));

        Quantity requested = Quantity.of(command.quantity());
        item.reserve(command.serviceOrderId(), requested, clock);

        repository.save(item);
        // The audited quantity is what this command added, not the total the order now holds: a
        // ledger records movements. The running total is the aggregate's business.
        auditTrail.append(InventoryAuditEntry.movement(item.getId(), AuditedOperation.RESERVED,
                command.serviceOrderId(), requested, command.actor(), clock.instant()));

        log.info("Material reserved: id={} name={} serviceOrderId={} reserved={} available={}",
                item.getId(), item.getName(), command.serviceOrderId(), requested,
                item.quantityAvailable());
        return item;
    }
}
