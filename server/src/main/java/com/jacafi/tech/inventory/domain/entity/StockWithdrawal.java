package com.jacafi.tech.inventory.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockWithdrawal(UUID inventoryItemId, UUID serviceOrderId, Stock quantity, Instant withdrawnAt) {
    public StockWithdrawal {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
        if (!quantity.isPositive()) throw new IllegalArgumentException("quantity must be positive");
    }
}
