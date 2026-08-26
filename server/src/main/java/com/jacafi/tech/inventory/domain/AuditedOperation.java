package com.jacafi.tech.inventory.domain;

/**
 * The write operations recorded in the audit trail.
 *
 * <p>Named in the past tense, after the domain events of the Event Storming board — the trail
 * records what happened, not what was asked for.
 *
 * <table border="1">
 *   <caption>Command, event and audited operation</caption>
 *   <tr><th>Command</th><th>Event</th><th>Recorded as</th></tr>
 *   <tr><td>{@code RegisterMaterial}</td><td>{@code MaterialRegistered}</td><td>{@link #REGISTERED}</td></tr>
 *   <tr><td>{@code UpdateMaterial}</td><td>{@code MaterialUpdated}</td><td>{@link #UPDATED}</td></tr>
 *   <tr><td>{@code RemoveMaterial}</td><td>{@code MaterialRemoved}</td><td>{@link #REMOVED}</td></tr>
 *   <tr><td>{@code ReplenishStock}</td><td>{@code StockReplenished}</td><td>{@link #REPLENISHED}</td></tr>
 *   <tr><td>{@code ReserveMaterial}</td><td>{@code MaterialReserved}</td><td>{@link #RESERVED}</td></tr>
 *   <tr><td>{@code ReleaseReservation}</td><td>{@code ReservationReleased}</td><td>{@link #RELEASED}</td></tr>
 *   <tr><td>{@code WithdrawMaterial}</td><td>{@code MaterialWithdrawn}</td><td>{@link #WITHDRAWN}</td></tr>
 * </table>
 */
public enum AuditedOperation {
    REGISTERED,
    UPDATED,
    REMOVED,
    REPLENISHED,
    RESERVED,
    RELEASED,
    WITHDRAWN
}
