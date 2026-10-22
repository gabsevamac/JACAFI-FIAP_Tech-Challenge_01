package com.jacafi.tech.laboroperation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
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

    @Test
    void updateNormalizesTheNewValuesAndMovesUpdatedAt() {
        LaborOperation operation =
                LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK);

        operation.update(
                "  Synthetic   oil change  ",
                "  Full synthetic.  ",
                new BigDecimal("129.9"),
                Clock.offset(CLOCK, Duration.ofHours(1)));

        assertThat(operation.name()).isEqualTo("Synthetic oil change");
        assertThat(operation.description()).isEqualTo("Full synthetic.");
        assertThat(operation.basePrice()).isEqualTo(new BigDecimal("129.90"));
        assertThat(operation.updatedAt()).isEqualTo(Instant.parse("2026-08-27T11:00:00Z"));
    }

    @Test
    void restoreRebuildsThePersistedState() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-02T10:00:00Z");

        LaborOperation operation =
                LaborOperation.restore(id, "Oil change", null, new BigDecimal("89.90"), false, 3, createdAt, updatedAt);

        assertThat(operation.id()).isEqualTo(id);
        assertThat(operation.active()).isFalse();
        assertThat(operation.version()).isEqualTo(3);
        assertThat(operation.createdAt()).isEqualTo(createdAt);
        assertThat(operation.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void restoreRejectsANegativeVersion() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");

        assertThatThrownBy(() -> LaborOperation.restore(
                        UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), true, -1, now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankOrOverlongNames() {
        assertThatThrownBy(
                        () -> LaborOperation.register(UUID.randomUUID(), "   ", null, new BigDecimal("89.90"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LaborOperation.register(
                        UUID.randomUUID(), "x".repeat(121), null, new BigDecimal("89.90"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
