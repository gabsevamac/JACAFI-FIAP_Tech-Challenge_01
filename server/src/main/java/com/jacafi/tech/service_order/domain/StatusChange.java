package com.jacafi.tech.service_order.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object recording a status transition in the lifecycle of a service order.
 *
 * <p>Section §4 and §9 of the ubiquitous language dictionary. Allows auditing the sequence of states
 * and calculating duration metrics (e.g. average service execution time) reliably.
 */
public record StatusChange(
        ServiceOrderStatus fromStatus, ServiceOrderStatus toStatus, Instant occurredAt, String actor) {

    public StatusChange {
        Objects.requireNonNull(toStatus, "toStatus must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static StatusChange initial(ServiceOrderStatus initialStatus, Instant occurredAt, String actor) {
        return new StatusChange(null, initialStatus, occurredAt, actor != null ? actor : "system");
    }

    public static StatusChange transition(
            ServiceOrderStatus fromStatus, ServiceOrderStatus toStatus, Instant occurredAt, String actor) {
        Objects.requireNonNull(fromStatus, "fromStatus must not be null");
        return new StatusChange(fromStatus, toStatus, occurredAt, actor != null ? actor : "system");
    }
}
