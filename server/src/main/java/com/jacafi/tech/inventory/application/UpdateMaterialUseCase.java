package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Corrects the name and the unit price of a material — the {@code UpdateMaterial} command.
 *
 * <p>A removed item is not found here, rather than found and rejected: to any caller, a removed
 * item and one that never existed are the same thing.
 *
 * <p>Renaming is subject to the same uniqueness rule as registering, minus the item itself — an
 * item keeping its own name is not a duplicate of itself.
 */
@Service
public class UpdateMaterialUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateMaterialUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public UpdateMaterialUseCase(InventoryItemRepository repository,
                                 InventoryAuditTrail auditTrail,
                                 Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public InventoryItem update(UpdateMaterialCommand command) {
        InventoryItem item = repository.findActiveByIdForUpdate(command.inventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException(command.inventoryItemId()));

        item.update(command.name(), command.unitPrice(), clock);

        if (repository.existsActiveWithNameExcluding(item.getName(), item.getId())) {
            throw new DuplicateMaterialException(
                    "A material with this name is already registered: " + item.getName());
        }

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.of(item.getId(), AuditedOperation.UPDATED,
                command.actor(), clock.instant()));

        log.info("Material updated: id={} name={}", item.getId(), item.getName());
        return item;
    }
}
