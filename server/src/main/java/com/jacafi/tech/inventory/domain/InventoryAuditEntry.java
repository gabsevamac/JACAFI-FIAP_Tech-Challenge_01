package com.jacafi.tech.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One line of the audit trail: who moved what, for which order, and when.
 *
 * <p>Required by Art. 37 of the LGPD, which obliges the controller to keep a record of the
 * processing operations it carries out. Here it earns its keep twice over, because it is also the
 * stock ledger: the aggregate holds a balance, and this is the only place that says how the
 * balance got there. Without it, "controle de estoque" would mean a number nobody can explain.
 *
 * <p>{@code serviceOrderId} and {@code quantity} are present on movements and absent on catalogue
 * operations — reserving names an order and a number of units, renaming a material names neither.
 * Modelled as two factories rather than one constructor with nulls at the call site, so that an
 * entry that should carry an order can never be written without one.
 *
 * @param inventoryItemId the item the operation acted upon
 * @param operation       what happened
 * @param serviceOrderId  the order behind a movement, absent for catalogue operations
 * @param quantity        units moved, absent for catalogue operations
 * @param actor           who did it, as identified by the subject of the JWT
 * @param occurredAt      when it happened
 */
public record InventoryAuditEntry(UUID inventoryItemId,
                                  AuditedOperation operation,
                                  UUID serviceOrderId,
                                  Quantity quantity,
                                  String actor,
                                  Instant occurredAt) {

    public InventoryAuditEntry {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
    }

    /** A catalogue operation: registered, updated, removed. */
    public static InventoryAuditEntry of(UUID inventoryItemId, AuditedOperation operation,
                                         String actor, Instant occurredAt) {
        return new InventoryAuditEntry(inventoryItemId, operation, null, null, actor, occurredAt);
    }

    /** A stock movement tied to a service order: reserved, released, withdrawn. */
    public static InventoryAuditEntry movement(UUID inventoryItemId, AuditedOperation operation,
                                               UUID serviceOrderId, Quantity quantity,
                                               String actor, Instant occurredAt) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null for a movement");
        Objects.requireNonNull(quantity, "quantity must not be null for a movement");
        return new InventoryAuditEntry(inventoryItemId, operation, serviceOrderId, quantity, actor, occurredAt);
    }

    /** A replenishment moves stock without an order behind it: the shelf is filled, not promised. */
    public static InventoryAuditEntry replenishment(UUID inventoryItemId, Quantity quantity,
                                                    String actor, Instant occurredAt) {
        Objects.requireNonNull(quantity, "quantity must not be null for a replenishment");
        return new InventoryAuditEntry(inventoryItemId, AuditedOperation.REPLENISHED, null, quantity,
                actor, occurredAt);
    }

    public Optional<UUID> optionalServiceOrderId() {
        return Optional.ofNullable(serviceOrderId);
    }

    public Optional<Quantity> optionalQuantity() {
        return Optional.ofNullable(quantity);
    }
}
