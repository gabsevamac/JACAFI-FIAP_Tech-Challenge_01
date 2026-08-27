package com.jacafi.tech.service_order.domain;

/**
 * State in which a service order is found.
 *
 * <p>Section §4 and §9 of the ubiquitous language dictionary:
 * <ul>
 *   <li>{@link #RECEIVED}: Service order opened, vehicle received by the workshop.</li>
 *   <li>{@link #UNDER_DIAGNOSIS}: Diagnosis in progress by the technician.</li>
 *   <li>{@link #AWAITING_APPROVAL}: Estimate generated and sent to the customer, awaiting approval.</li>
 *   <li>{@link #IN_PROGRESS}: Estimate approved by the customer, services being executed.</li>
 *   <li>{@link #COMPLETED}: All services finished, materials withdrawn, ready for delivery.</li>
 *   <li>{@link #DELIVERED}: Vehicle delivered to the customer (terminal state).</li>
 *   <li>{@link #REJECTED}: Estimate rejected by the customer or expired (terminal state).</li>
 * </ul>
 */
public enum ServiceOrderStatus {
    RECEIVED,
    UNDER_DIAGNOSIS,
    AWAITING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED,
    REJECTED;

    /** Returns true if this status is a terminal state in the lifecycle of a service order. */
    public boolean isTerminal() {
        return this == DELIVERED || this == REJECTED;
    }
}
