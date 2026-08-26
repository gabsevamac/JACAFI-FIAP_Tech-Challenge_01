package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Writes a part or a supply into the catalogue — the {@code RegisterMaterial} command of the
 * Event Storming board, driven by the Manager.
 *
 * <p>This is where name uniqueness is enforced. The aggregate cannot do it: uniqueness spans the
 * whole catalogue, and an aggregate only guards its own state. Doing the check here is also what
 * turns a duplicate into a business rule violation with a name, instead of a constraint violation
 * surfacing from the driver.
 *
 * <p>The partial unique index in the database still matters, for two concurrent registrations of
 * the same name that both pass this check. That case belongs to the persistence layer, which
 * translates the violation back into {@link DuplicateMaterialException}.
 */
@Service
public class RegisterMaterialUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterMaterialUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public RegisterMaterialUseCase(InventoryItemRepository repository,
                                   InventoryAuditTrail auditTrail,
                                   Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public InventoryItem register(RegisterMaterialCommand command) {
        InventoryItem item = InventoryItem.builder()
                .id(UUID.randomUUID())
                .name(command.name())
                .type(command.type())
                .unitPrice(command.unitPrice())
                .stockOnHand(Quantity.of(command.initialStock()))
                .register(clock);

        // Checked against the normalized name the aggregate produced, not the raw input: "Filtro
        // de  óleo" and "filtro de óleo" are the same material to everyone but a string compare.
        if (repository.existsActiveWithName(item.getName())) {
            throw new DuplicateMaterialException(
                    "A material with this name is already registered: " + item.getName());
        }

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.of(item.getId(), AuditedOperation.REGISTERED,
                command.actor(), clock.instant()));

        log.info("Material registered: id={} name={} type={} initialStock={}",
                item.getId(), item.getName(), item.getType(), item.getStockOnHand());
        return item;
    }
}
