package com.jacafi.tech.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A quantity of one stock item held for one service order, taken out of what is available to
 * everyone else but still physically on the shelf.
 *
 * <p>Entity internal to {@link InventoryItem}: it has an identity of its own and a life of its
 * own — created on approval, ended by a release or by a withdrawal — but it is never loaded,
 * stored or referenced from outside the aggregate that owns it.
 *
 * <p>The reservation is what answers the question the whole slice exists for: which order
 * authorized this material to leave. A withdrawal with no reservation behind it is a part leaving
 * the shelf without an approved order, which is precisely what the domain vision forbids.
 *
 * <p>Immutable, like every value the aggregate holds. {@link #increasedBy(Quantity)} returns a new
 * instance keeping the same identity and the original instant on purpose: an additional repair
 * enlarges a reservation that already exists rather than replacing it, and preserving the identity
 * is what lets storage update the row instead of deleting one and inserting another.
 *
 * @param id             identity of the reservation itself
 * @param serviceOrderId the order that authorized it, by identifier — {@code ServiceOrder} is the
 *                       root of another aggregate, owned by another slice
 * @param quantity       units held, always at least one
 * @param reservedAt     when the reservation was first created
 */
public record Reservation(UUID id, UUID serviceOrderId, Quantity quantity, Instant reservedAt) {

    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(reservedAt, "reservedAt must not be null");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("A reservation must hold at least one unit");
        }
    }

    static Reservation open(UUID serviceOrderId, Quantity quantity, Instant reservedAt) {
        return new Reservation(UUID.randomUUID(), serviceOrderId, quantity, reservedAt);
    }

    /** Same identity, same instant, more units — an additional repair on the same order. */
    Reservation increasedBy(Quantity extra) {
        return new Reservation(id, serviceOrderId, quantity.plus(extra), reservedAt);
    }
}
