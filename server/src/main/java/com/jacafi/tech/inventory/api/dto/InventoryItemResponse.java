package com.jacafi.tech.inventory.api.dto;

import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.MaterialType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A stock item as the API exposes it. The aggregate never leaves the application through the wire.
 *
 * <p>The three quantities are all reported, and none of them is redundant: on hand is what is
 * physically there, reserved is what other orders are counting on, available is what a new order
 * could still be promised. A screen showing only the first would let an advisor promise a part
 * that is already spoken for.
 *
 * <p>Nothing here is personal data. A material name, a price and a count identify no one, which is
 * why this slice carries no {@code PersonalData} marker and no masking.
 */
public record InventoryItemResponse(
        UUID id,
        String name,
        MaterialType type,
        BigDecimal unitPrice,
        @Schema(description = "Units physically on the shelf, reserved ones included") int stockOnHand,
        @Schema(description = "Units held by open reservations") int stockReserved,
        @Schema(description = "Units a new order could still be promised") int stockAvailable,
        List<ReservationResponse> reservations,
        Instant registeredAt,
        Instant updatedAt) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.getId(),
                item.getName(),
                item.getType(),
                item.getUnitPrice(),
                item.getStockOnHand().value(),
                item.stockReserved().value(),
                item.stockAvailable().value(),
                item.getReservations().stream().map(ReservationResponse::from).toList(),
                item.getRegisteredAt(),
                item.getUpdatedAt());
    }
}
