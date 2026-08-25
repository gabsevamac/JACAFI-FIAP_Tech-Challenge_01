package com.jacafi.tech.vehicle.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference from a vehicle to the customer responsible for it.
 *
 * <p>A value object, not an object reference: {@code Customer} is the root of a separate
 * aggregate, owned by another slice, and a slice never imports another slice's domain. Holding
 * the identifier is what keeps the two aggregates independently consistent — and what allows the
 * customer slice to be developed in parallel without either side waiting on the other.
 *
 * <p>Wrapping the {@link UUID} rather than passing it raw keeps a customer identifier from being
 * silently accepted where a vehicle identifier belongs.
 *
 * @param value the customer's identifier, as assigned by the customer slice
 */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "customerId must not be null");
    }
}
