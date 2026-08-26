package com.jacafi.tech.inventory.domain;

/**
 * More units were asked for than the item has available.
 *
 * <p>Available is what is on hand minus what is already reserved for other orders — the units are
 * still on the shelf, but they are spoken for. Refusing here is what stops the same part from
 * being promised to two orders, which the workshop would only discover with the vehicle already
 * on the lift.
 *
 * <p>Carries both numbers: a caller that cannot see how short it is has to guess, and the
 * quantities are operational data, not personal data.
 */
public class InsufficientStockException extends RuntimeException {

    private final int requested;
    private final int available;

    public InsufficientStockException(Quantity requested, Quantity available) {
        super("Requested %d units but only %d are available"
                .formatted(requested.value(), available.value()));
        this.requested = requested.value();
        this.available = available.value();
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
