package com.jacafi.tech.inventory.application;

import java.util.UUID;

/**
 * Input for adding units to the shelf.
 *
 * <p>No supplier, no invoice, no cost. Purchasing is outside this bounded context (§2 of the
 * dictionary); what reaches here is only the fact that the units arrived.
 *
 * @param actor who is performing the operation, taken from the JWT subject by the api layer
 */
public record ReplenishStockCommand(UUID inventoryItemId, int quantity, String actor) {
}
