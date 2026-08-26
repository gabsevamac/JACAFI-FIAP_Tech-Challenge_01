package com.jacafi.tech.inventory.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.inventory.domain.Reservation;

/** An open reservation as the API exposes it. */
public record ReservationResponse(UUID id, UUID serviceOrderId, int quantity, Instant reservedAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id(),
                reservation.serviceOrderId(),
                reservation.quantity().value(),
                reservation.reservedAt());
    }
}
