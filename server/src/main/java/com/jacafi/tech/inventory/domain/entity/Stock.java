package com.jacafi.tech.inventory.domain.entity;

/** Non-negative number of stock units. */
public record Stock(int value) {
    public static final Stock ZERO = new Stock(0);

    public Stock {
        if (value < 0) throw new IllegalArgumentException("stock must not be negative");
    }

    public static Stock of(int value) {
        return value == 0 ? ZERO : new Stock(value);
    }

    public Stock plus(Stock other) {
        return new Stock(Math.addExact(value, other.value));
    }

    public Stock minus(Stock other) {
        return new Stock(Math.subtractExact(value, other.value));
    }

    public boolean isPositive() {
        return value > 0;
    }

    public boolean isLessThan(Stock other) {
        return value < other.value;
    }
}
