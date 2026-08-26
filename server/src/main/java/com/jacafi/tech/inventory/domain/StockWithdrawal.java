package com.jacafi.tech.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Baixa: the record of material leaving the shelf because it was applied to a vehicle.
 *
 * <p>A value object, and the outcome of {@link InventoryItem#withdraw}. It always names the
 * service order it belongs to, because there is no other legitimate reason for stock to go down —
 * "nenhuma peça sai do estoque sem vínculo com uma ordem aprovada" is the sentence this type
 * makes representable.
 *
 * <p>Returned by the aggregate rather than stored inside it. Withdrawals accumulate forever, and
 * an aggregate that carried its whole history would have to be loaded in full to reserve a single
 * unit. The history lives in the audit trail, which is append-only and made for it.
 *
 * @param inventoryItemId the item whose stock went down
 * @param serviceOrderId  the order the material was applied to
 * @param quantity        units withdrawn, always at least one
 * @param withdrawnAt     when it happened
 */
public record StockWithdrawal(UUID inventoryItemId, UUID serviceOrderId, Quantity quantity, Instant withdrawnAt) {

    public StockWithdrawal {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("A withdrawal must move at least one unit");
        }
    }
}
