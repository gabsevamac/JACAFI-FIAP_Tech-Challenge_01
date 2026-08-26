package com.jacafi.tech.inventory.domain;

/**
 * Whether a stock item is a part or a supply.
 *
 * <p>Both words were kept because the workshop distinguishes them in speech and the distinction
 * has a consequence: a part stays identifiable in the vehicle and carries a warranty attached to
 * it, a supply is consumed during the work and does not. See Q4 in §8 of the dictionary — if the
 * distinction never leads to a different decision, the two collapse into one concept and this
 * enum becomes a label.
 *
 * <p>Immutable once registered. A part does not become a supply; recording the wrong one is a
 * registration mistake, and the fix is to remove the item and register the right one, not to
 * reinterpret the history of every withdrawal already made against it.
 */
public enum MaterialType {

    /** Peça — a physical component applied to the vehicle, where it remains identifiable. */
    PART,

    /** Insumo — material consumed while a service is performed, leaving nothing identifiable. */
    SUPPLY
}
