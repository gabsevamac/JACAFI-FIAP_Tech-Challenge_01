package com.jacafi.tech.laboroperation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LaborOperationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersAnActiveItemWithNormalizedMoneyAndText() {
        LaborOperation operation = LaborOperation.register(
                UUID.randomUUID(), "  Oil change  ", "  Replace engine oil.  ", new BigDecimal("89.90"), CLOCK);

        assertThat(operation.name()).isEqualTo("Oil change");
        assertThat(operation.description()).isEqualTo("Replace engine oil.");
        assertThat(operation.basePrice()).isEqualByComparingTo("89.90");
        assertThat(operation.active()).isTrue();
        assertThat(operation.createdAt()).isEqualTo(Instant.parse("2026-08-27T10:00:00Z"));
    }

    @Test
    void rejectsNegativeOrFractionallyInvalidPrices() {
        assertThatThrownBy(() ->
                        LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("-0.01"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("1.999"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivatedItemCannotBeUpdated() {
        LaborOperation operation =
                LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK);

        operation.deactivate(CLOCK);

        assertThat(operation.active()).isFalse();
        assertThatThrownBy(() -> operation.update("Premium oil change", null, new BigDecimal("99.90"), CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }
}
