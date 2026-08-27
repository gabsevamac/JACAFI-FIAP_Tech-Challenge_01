package com.jacafi.tech.inventory.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Open allocation of an inventory item to one service order. */
public record Reservation(UUID id, UUID serviceOrderId, Stock quantity, Instant reservedAt) {
    public Reservation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(reservedAt, "reservedAt must not be null");
        if (!quantity.isPositive()) throw new IllegalArgumentException("quantity must be positive");
    }

    static Reservation open(UUID serviceOrderId, Stock quantity, Instant reservedAt) {
        return new Reservation(UUID.randomUUID(), serviceOrderId, quantity, reservedAt);
    }

    Reservation increasedBy(Stock quantity) {
        return new Reservation(id, serviceOrderId, this.quantity.plus(quantity), reservedAt);
    }
}
