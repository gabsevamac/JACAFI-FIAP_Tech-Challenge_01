package com.jacafi.tech.inventory.domain;

/**
 * A count of units of one stock item: never negative, equal by value, immutable.
 *
 * <p>Exists because this slice does arithmetic on three quantities that look alike — on hand,
 * reserved and available — and an {@code int} would let any of them go negative silently. A
 * negative stock level is not a state the workshop can be in: it means the books say something
 * the shelf cannot confirm, which is the exact failure this slice was built to end.
 *
 * <p>Addition is checked rather than wrapping. Overflow is unreachable with real quantities, and
 * an unreachable case that silently produces a negative number is worse than one that throws.
 *
 * <p>Named after what it counts rather than after the act of counting. §9 of the dictionary
 * settles the word in the Estoque entry — "{@code stock} para a quantidade" — and the slice
 * follows it into the type itself: everything measured here is stock, whether it is sitting on
 * the shelf, held by a reservation or leaving in a withdrawal.
 *
 * <p>The alternative considered and rejected on review was {@code Quantity}. It answers "a
 * quantity of what?" with nothing, and would fit a codebase about anything at all; a name that
 * carries the domain is worth more than one that is merely accurate. The component names stay
 * with the role each amount plays — a reservation holds a {@code quantity} of type {@code Stock}
 * — so the type says what is being counted and the field says why.
 *
 * @param value the number of units, zero or more
 */
public record Stock(int value) {

    public static final Stock ZERO = new Stock(0);

    public Stock {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
    }

    public static Stock of(int value) {
        return value == 0 ? ZERO : new Stock(value);
    }

    public Stock plus(Stock other) {
        return new Stock(Math.addExact(value, other.value));
    }

    /**
     * @throws IllegalArgumentException when the result would be negative, which every caller here
     *                                  guards against beforehand with a message of its own
     */
    public Stock minus(Stock other) {
        return new Stock(value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isPositive() {
        return value > 0;
    }

    public boolean isLessThan(Stock other) {
        return value < other.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
