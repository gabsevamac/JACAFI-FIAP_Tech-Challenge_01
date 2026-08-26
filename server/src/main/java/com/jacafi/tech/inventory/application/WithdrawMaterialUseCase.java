package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryAuditTrail;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.StockWithdrawal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Takes the reserved units off the shelf — the {@code WithdrawMaterial} command, driven by
 * {@code WhenServicesCompleted}. This is the baixa of §5 of the dictionary.
 *
 * <p>It withdraws against a reservation and nothing else. An order with no reservation here never
 * had an approved estimate covering this material, and there is no path through this use case
 * that would let it take any — which is the domain vision stated as code: "nenhuma peça sai do
 * estoque sem vínculo com uma ordem aprovada".
 *
 * <p>The withdrawal returned is the record of what left; the trail entry is its permanent copy.
 * The aggregate keeps only the balance.
 */
@Service
public class WithdrawMaterialUseCase {

    private static final Logger log = LoggerFactory.getLogger(WithdrawMaterialUseCase.class);

    private final InventoryItemRepository repository;
    private final InventoryAuditTrail auditTrail;
    private final Clock clock;

    public WithdrawMaterialUseCase(InventoryItemRepository repository,
                                   InventoryAuditTrail auditTrail,
                                   Clock clock) {
        this.repository = repository;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Transactional
    public StockWithdrawal withdraw(UUID inventoryItemId, UUID serviceOrderId, String actor) {
        InventoryItem item = repository.findActiveByIdForUpdate(inventoryItemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(inventoryItemId));

        StockWithdrawal withdrawal = item.withdraw(serviceOrderId, clock);

        repository.save(item);
        auditTrail.append(InventoryAuditEntry.movement(item.getId(), AuditedOperation.WITHDRAWN,
                serviceOrderId, withdrawal.quantity(), actor, clock.instant()));

        log.info("Material withdrawn: id={} name={} serviceOrderId={} withdrawn={} onHand={}",
                item.getId(), item.getName(), serviceOrderId, withdrawal.quantity(),
                item.getQuantityOnHand());
        return withdrawal;
    }
}
