package com.jacafi.tech.inventory.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.inventory.domain.exception.InsufficientStockException;
import com.jacafi.tech.inventory.domain.exception.ReservationNotFoundException;

class InventoryItemTest {
    private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Test
    void reservesReleasesAndWithdrawsOnlyReservedStock() {
        InventoryItem item = item(5);
        UUID order = UUID.randomUUID();
        item.reserve(order, Stock.of(3), CLOCK);
        assertThat(item.stockAvailable()).isEqualTo(Stock.of(2));
        assertThat(item.releaseReservation(order, CLOCK).quantity()).isEqualTo(Stock.of(3));
        item.reserve(order, Stock.of(2), CLOCK);
        assertThat(item.withdraw(order, CLOCK).quantity()).isEqualTo(Stock.of(2));
        assertThat(item.stockOnHand()).isEqualTo(Stock.of(3));
        assertThatThrownBy(() -> item.withdraw(order, CLOCK)).isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void refusesOversellingAndRemovingReservedItem() {
        InventoryItem item = item(1);
        item.reserve(UUID.randomUUID(), Stock.of(1), CLOCK);
        assertThatThrownBy(() -> item.reserve(UUID.randomUUID(), Stock.of(1), CLOCK))
                .isInstanceOf(InsufficientStockException.class);
        assertThatThrownBy(() -> item.remove(CLOCK)).isInstanceOf(IllegalStateException.class);
    }

    private static InventoryItem item(int stock) {
        return InventoryItem.register(
                UUID.randomUUID(), "Oil filter", MaterialType.PART, new BigDecimal("20.00"), Stock.of(stock), CLOCK);
    }
}
