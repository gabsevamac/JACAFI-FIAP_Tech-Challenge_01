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
 * <p>The type stays {@code Quantity} while the stock level it holds is named {@code stock},
 * which is what §9 of the dictionary fixes: "{@code stock} para a quantidade". The two are not
 * the same thing. A stock level is a quantity, but so is the amount a reservation holds and the
 * amount a withdrawal moves, and neither of those is stock — a baixa does not have a stock of
 * four. The dictionary settles the vocabulary of the domain, not the names of measurement types,
 * which is why {@code Instant} and {@code BigDecimal} are absent from it as well.
 *
 * @param value the number of units, zero or more
 */
public record Quantity(int value) {

    public static final Quantity ZERO = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
    }

    public static Quantity of(int value) {
        return value == 0 ? ZERO : new Quantity(value);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(Math.addExact(value, other.value));
    }

    /**
     * @throws IllegalArgumentException when the result would be negative, which every caller here
     *                                  guards against beforehand with a message of its own
     */
    public Quantity minus(Quantity other) {
        return new Quantity(value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isPositive() {
        return value > 0;
    }

    public boolean isLessThan(Quantity other) {
        return value < other.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
