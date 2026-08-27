package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;

public record InventoryItemResponse(
        UUID id,
        String name,
        MaterialType type,
        BigDecimal unitPrice,
        int stockOnHand,
        int stockReserved,
        int stockAvailable,
        List<ReservationResponse> reservations,
        Instant registeredAt,
        Instant updatedAt,
        long version) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.id(),
                item.name(),
                item.type(),
                item.unitPrice(),
                item.stockOnHand().value(),
                item.stockReserved().value(),
                item.stockAvailable().value(),
                item.reservations().stream().map(ReservationResponse::from).toList(),
                item.registeredAt(),
                item.updatedAt(),
                item.version());
    }
}
