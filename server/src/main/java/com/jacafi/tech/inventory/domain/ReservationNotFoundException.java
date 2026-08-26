package com.jacafi.tech.inventory.domain;

import java.util.UUID;

/**
 * The item holds no open reservation for the given service order.
 *
 * <p>Reached by a release or a withdrawal for an order that never reserved, or whose reservation
 * has already been settled. Both are the same answer to the caller: there is nothing here to act
 * on.
 */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID inventoryItemId, UUID serviceOrderId) {
        super("Inventory item %s holds no open reservation for service order %s"
                .formatted(inventoryItemId, serviceOrderId));
    }
}
