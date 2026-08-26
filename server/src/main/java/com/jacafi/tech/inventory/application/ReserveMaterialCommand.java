package com.jacafi.tech.inventory.application;

import java.util.UUID;

/**
 * Input for holding units for a service order.
 *
 * <p>{@code serviceOrderId} is an identifier and never an object: {@code ServiceOrder} is the root
 * of another aggregate, owned by another slice, and a slice does not import another slice's
 * domain. Holding the identifier is what lets the two stay independently consistent — which is the
 * whole reason {@code Inventory} is a separate aggregate.
 *
 * @param actor who is performing the operation, taken from the JWT subject by the api layer
 */
public record ReserveMaterialCommand(UUID inventoryItemId,
                                     UUID serviceOrderId,
                                     int quantity,
                                     String actor) {
}
