package com.jacafi.tech.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input for correcting the name and the unit price of a material.
 *
 * <p>No {@code MaterialType} and no quantity. The type is immutable after registration, and stock
 * moves only through the commands that name what moved it — replenishment, reservation,
 * withdrawal. A quantity field here would be a way to change the balance with nothing in the
 * ledger explaining why, which is the practice this slice exists to replace.
 *
 * @param actor who is performing the operation, taken from the JWT subject by the api layer
 */
public record UpdateMaterialCommand(UUID inventoryItemId, String name, BigDecimal unitPrice, String actor) {}
