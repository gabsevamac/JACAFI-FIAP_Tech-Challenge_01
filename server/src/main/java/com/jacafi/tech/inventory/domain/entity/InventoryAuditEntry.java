package com.jacafi.tech.inventory.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Append-only stock movement ledger entry. */
public record InventoryAuditEntry(
        UUID inventoryItemId,
        AuditedOperation operation,
        UUID serviceOrderId,
        Stock quantity,
        String actor,
        Instant occurredAt) {
    public InventoryAuditEntry {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("actor must not be blank");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        boolean movement = operation == AuditedOperation.RESERVED
                || operation == AuditedOperation.RELEASED
                || operation == AuditedOperation.WITHDRAWN;
        boolean replenishment = operation == AuditedOperation.REPLENISHED;
        if (movement && (serviceOrderId == null || quantity == null || !quantity.isPositive()))
            throw new IllegalArgumentException("movement entries require service order and positive quantity");
        if (replenishment && (serviceOrderId != null || quantity == null || !quantity.isPositive()))
            throw new IllegalArgumentException("replenishment requires a positive quantity only");
        if (!movement && !replenishment && (serviceOrderId != null || quantity != null))
            throw new IllegalArgumentException("catalogue entries must not contain movement data");
    }

    public static InventoryAuditEntry action(UUID id, AuditedOperation operation, String actor, Instant at) {
        return new InventoryAuditEntry(id, operation, null, null, actor, at);
    }

    public static InventoryAuditEntry replenishment(UUID id, Stock quantity, String actor, Instant at) {
        return new InventoryAuditEntry(id, AuditedOperation.REPLENISHED, null, quantity, actor, at);
    }

    public static InventoryAuditEntry movement(
            UUID id, AuditedOperation operation, UUID orderId, Stock quantity, String actor, Instant at) {
        return new InventoryAuditEntry(id, operation, orderId, quantity, actor, at);
    }
}
