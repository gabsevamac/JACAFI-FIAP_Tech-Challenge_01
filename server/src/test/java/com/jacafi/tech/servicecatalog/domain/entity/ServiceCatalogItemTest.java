package com.jacafi.tech.servicecatalog.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ServiceCatalogItemTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersAnActiveItemWithNormalizedMoneyAndText() {
        ServiceCatalogItem item = ServiceCatalogItem.register(
                UUID.randomUUID(), "  Oil change  ", "  Replace engine oil.  ", new BigDecimal("89.90"), CLOCK);

        assertThat(item.name()).isEqualTo("Oil change");
        assertThat(item.description()).isEqualTo("Replace engine oil.");
        assertThat(item.basePrice()).isEqualByComparingTo("89.90");
        assertThat(item.active()).isTrue();
        assertThat(item.createdAt()).isEqualTo(Instant.parse("2026-08-27T10:00:00Z"));
    }

    @Test
    void rejectsNegativeOrFractionallyInvalidPrices() {
        assertThatThrownBy(() -> ServiceCatalogItem.register(
                        UUID.randomUUID(), "Oil change", null, new BigDecimal("-0.01"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceCatalogItem.register(
                        UUID.randomUUID(), "Oil change", null, new BigDecimal("1.999"), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivatedItemCannotBeUpdated() {
        ServiceCatalogItem item =
                ServiceCatalogItem.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK);

        item.deactivate(CLOCK);

        assertThat(item.active()).isFalse();
        assertThatThrownBy(() -> item.update("Premium oil change", null, new BigDecimal("99.90"), CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }
}
