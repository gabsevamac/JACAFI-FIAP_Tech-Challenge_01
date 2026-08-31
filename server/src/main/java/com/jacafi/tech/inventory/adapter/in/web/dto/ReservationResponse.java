package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.inventory.domain.entity.Reservation;

public record ReservationResponse(UUID serviceOrderId, int quantity, Instant reservedAt) {
    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.serviceOrderId(), reservation.quantity().value(), reservation.reservedAt());
    }
}
