package com.jacafi.tech.inventory.application;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;

/**
 * Takes a material out of the catalogue — the {@code RemoveMaterial} command.
 *
 * <p>The row is not deleted. Withdrawals already recorded point at this item, and a ledger whose
 * entries reference something that no longer exists proves nothing about the stock it describes.
 * The item simply stops answering queries and stops accepting operations, which is what "out of
 * the catalogue" means to the workshop.
 *
 * <p>Nothing personal is erased: a material name and a count identify nobody, so unlike the
 * vehicle slice this removal answers no request from a data subject. It is an ordinary catalogue
 * decision, audited like every other write.
 *
 * <p>An item with open reservations is refused by the aggregate — see {@code InventoryItem#remove}.
 * Two removals in a row give 404 on the second, not a conflict: after the first, there is no
 * active item left to find.
 */
@Service
public class RemoveMaterialUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemoveMaterialUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public RemoveMaterialUseCase(InventoryItemRepository repository, InventoryAuditTrail auditTrail, Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public void remove(UUID inventoryItemId, String actor) {
        InventoryItem item = repository
                .findActiveByIdForUpdate(inventoryItemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(inventoryItemId));

        item.remove(clock);

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.of(item.getId(), AuditedOperation.REMOVED, actor, clock.instant()));

        log.info("Material removed from the catalogue: id={} name={}", item.getId(), item.getName());
    }
}
