package com.jacafi.tech.inventory.domain;

import java.util.UUID;

/**
 * No active stock item exists for the given identifier.
 *
 * <p>A removed item is indistinguishable from one that never existed: its row survives so that
 * past withdrawals keep pointing at something, but it answers no query and takes no operation.
 */
public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(UUID inventoryItemId) {
        super("No active inventory item found for id " + inventoryItemId);
    }
}
