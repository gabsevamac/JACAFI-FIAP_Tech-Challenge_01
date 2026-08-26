package com.jacafi.tech.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    @DisplayName("a negative quantity cannot be constructed at all")
    void rejectsNegative() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("subtracting past zero is refused rather than wrapping into a negative stock")
    void rejectsSubtractionBelowZero() {
        assertThatThrownBy(() -> Quantity.of(2).minus(Quantity.of(3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addition overflows loudly instead of producing a negative count")
    void rejectsOverflow() {
        assertThatThrownBy(() -> Quantity.of(Integer.MAX_VALUE).plus(Quantity.of(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void addsSubtractsAndCompares() {
        assertThat(Quantity.of(4).plus(Quantity.of(3)).value()).isEqualTo(7);
        assertThat(Quantity.of(4).minus(Quantity.of(3)).value()).isEqualTo(1);
        assertThat(Quantity.of(2).isLessThan(Quantity.of(3))).isTrue();
        assertThat(Quantity.ZERO.isZero()).isTrue();
        assertThat(Quantity.ZERO.isPositive()).isFalse();
    }
}
