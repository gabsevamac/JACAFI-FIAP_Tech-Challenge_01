package com.jacafi.tech.inventory.application;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.Stock;

/**
 * Adds units to the shelf — the {@code ReplenishStock} command, driven by the Manager.
 *
 * <p>The only movement in this slice with no service order behind it, and the reason the audit
 * trail has a factory of its own for it: stock going up is the workshop buying, stock going down
 * is a customer's approved order. Conflating the two would make the ledger unreadable in exactly
 * the case someone consults it — explaining a balance that looks wrong.
 */
@Service
public class ReplenishStockUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReplenishStockUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public ReplenishStockUseCase(InventoryItemRepository repository, InventoryAuditTrail auditTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public InventoryItem replenish(ReplenishStockCommand command) {
        InventoryItem item = repository
                .findActiveByIdForUpdate(command.inventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException(command.inventoryItemId()));

        Stock added = Stock.of(command.quantity());
        item.replenish(added, clock);

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.replenishment(item.getId(), added, command.actor(), clock.instant()));

        log.info(
                "Stock replenished: id={} name={} added={} onHand={}",
                item.getId(),
                item.getName(),
                added,
                item.getStockOnHand());
        return item;
    }
}
