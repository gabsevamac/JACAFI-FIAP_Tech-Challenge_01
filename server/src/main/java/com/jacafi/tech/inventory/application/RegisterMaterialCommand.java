package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.MaterialType;

import java.math.BigDecimal;

/**
 * Input for registering a part or a supply.
 *
 * <p>Carries primitives, not value objects: it crosses the boundary inward from a caller that has
 * only strings and numbers, and turning those into a {@code Quantity} is the use case's job —
 * which is also where the resulting validation failure belongs.
 *
 * @param initialQuantity the opening balance, what is already on the shelf when the material is
 *                        first written down; zero for a material the workshop has yet to buy
 * @param actor           who is performing the operation, taken from the JWT subject by the api layer
 */
public record RegisterMaterialCommand(String name,
                                      MaterialType type,
                                      BigDecimal unitPrice,
                                      int initialQuantity,
                                      String actor) {
}
