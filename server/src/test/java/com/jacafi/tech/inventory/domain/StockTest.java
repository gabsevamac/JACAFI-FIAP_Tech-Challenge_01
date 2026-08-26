package com.jacafi.tech.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    @DisplayName("a negative quantity cannot be constructed at all")
    void rejectsNegative() {
        assertThatThrownBy(() -> Stock.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("subtracting past zero is refused rather than wrapping into a negative stock")
    void rejectsSubtractionBelowZero() {
        assertThatThrownBy(() -> Stock.of(2).minus(Stock.of(3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addition overflows loudly instead of producing a negative count")
    void rejectsOverflow() {
        assertThatThrownBy(() -> Stock.of(Integer.MAX_VALUE).plus(Stock.of(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void addsSubtractsAndCompares() {
        assertThat(Stock.of(4).plus(Stock.of(3)).value()).isEqualTo(7);
        assertThat(Stock.of(4).minus(Stock.of(3)).value()).isEqualTo(1);
        assertThat(Stock.of(2).isLessThan(Stock.of(3))).isTrue();
        assertThat(Stock.ZERO.isZero()).isTrue();
        assertThat(Stock.ZERO.isPositive()).isFalse();
    }
}
