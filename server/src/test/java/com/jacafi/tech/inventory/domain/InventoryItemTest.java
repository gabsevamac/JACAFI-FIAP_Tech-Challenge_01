package com.jacafi.tech.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The rules of the aggregate, without Spring, without a database and without a clock that moves on
 * its own. Every assertion here is about the domain and would survive replacing every framework
 * around it.
 */
class InventoryItemTest {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID ORDER = UUID.randomUUID();
    private static final UUID OTHER_ORDER = UUID.randomUUID();

    private static InventoryItem item(int onHand) {
        return InventoryItem.builder()
                .id(UUID.randomUUID())
                .name("Filtro de óleo")
                .type(MaterialType.PART)
                .unitPrice(new BigDecimal("49.90"))
                .stockOnHand(Stock.of(onHand))
                .register(CLOCK);
    }

    @Nested
    @DisplayName("registration")
    class Registration {

        @Test
        @DisplayName("stamps both timestamps from the clock and starts with no reservations")
        void registers() {
            InventoryItem item = item(10);

            assertThat(item.getRegisteredAt()).isEqualTo(NOW);
            assertThat(item.getUpdatedAt()).isEqualTo(NOW);
            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(10));
            assertThat(item.stockReserved()).isEqualTo(Stock.ZERO);
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(10));
            assertThat(item.getReservations()).isEmpty();
            assertThat(item.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("normalizes the name, so trailing and repeated whitespace never reaches storage")
        void normalizesName() {
            InventoryItem item = InventoryItem.builder()
                    .id(UUID.randomUUID())
                    .name("  Filtro   de  óleo ")
                    .type(MaterialType.PART)
                    .unitPrice(BigDecimal.ZERO)
                    .register(CLOCK);

            assertThat(item.getName()).isEqualTo("Filtro de óleo");
        }

        @Test
        @DisplayName("a price with more decimals than money has is refused, not rounded")
        void rejectsUnchargeablePrice() {
            assertThatThrownBy(() -> InventoryItem.builder()
                            .id(UUID.randomUUID())
                            .name("Óleo 5W30")
                            .type(MaterialType.SUPPLY)
                            .unitPrice(new BigDecimal("39.999"))
                            .register(CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("two decimal places");
        }

        @Test
        void rejectsBlankNameAndNegativePrice() {
            assertThatThrownBy(() -> InventoryItem.builder()
                            .id(UUID.randomUUID())
                            .name("   ")
                            .type(MaterialType.PART)
                            .unitPrice(BigDecimal.ONE)
                            .register(CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");

            assertThatThrownBy(() -> InventoryItem.builder()
                            .id(UUID.randomUUID())
                            .name("Correia")
                            .type(MaterialType.PART)
                            .unitPrice(new BigDecimal("-0.01"))
                            .register(CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("zero or positive");
        }

        @Test
        @DisplayName("timestamps belong to restore: registering with one is a programming error")
        void rejectsTimestampsOnRegistration() {
            assertThatThrownBy(() -> InventoryItem.builder()
                            .id(UUID.randomUUID())
                            .name("Correia")
                            .type(MaterialType.PART)
                            .unitPrice(BigDecimal.ONE)
                            .registeredAt(NOW)
                            .register(CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("restore only");
        }
    }

    @Nested
    @DisplayName("stock movement")
    class StockMovement {

        @Test
        @DisplayName("replenishing raises what is on hand and what is available alike")
        void replenishes() {
            InventoryItem item = item(4);
            item.replenish(Stock.of(6), CLOCK);

            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(10));
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(10));
        }

        @Test
        void rejectsAReplenishmentOfNothing() {
            assertThatThrownBy(() -> item(4).replenish(Stock.ZERO, CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one unit");
        }

        @Test
        @DisplayName("reserving holds units without taking anything off the shelf")
        void reservesWithoutMovingStock() {
            InventoryItem item = item(10);
            Reservation reservation = item.reserve(ORDER, Stock.of(3), CLOCK);

            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(10));
            assertThat(item.stockReserved()).isEqualTo(Stock.of(3));
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(7));
            assertThat(reservation.serviceOrderId()).isEqualTo(ORDER);
            assertThat(reservation.reservedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("what another order already holds is not available to this one")
        void refusesToPromiseUnitsAnotherOrderHolds() {
            InventoryItem item = item(5);
            item.reserve(OTHER_ORDER, Stock.of(4), CLOCK);

            assertThatThrownBy(() -> item.reserve(ORDER, Stock.of(2), CLOCK))
                    .isInstanceOf(InsufficientStockException.class)
                    .satisfies(thrown -> {
                        InsufficientStockException e = (InsufficientStockException) thrown;
                        assertThat(e.getRequested()).isEqualTo(2);
                        assertThat(e.getAvailable()).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("an additional repair enlarges the order's reservation instead of opening a second")
        void mergesASecondReservationForTheSameOrder() {
            InventoryItem item = item(10);
            Reservation first = item.reserve(ORDER, Stock.of(2), CLOCK);

            Clock later = Clock.fixed(NOW.plus(Duration.ofHours(3)), ZoneOffset.UTC);
            Reservation enlarged = item.reserve(ORDER, Stock.of(3), later);

            assertThat(item.getReservations()).hasSize(1);
            assertThat(enlarged.id()).isEqualTo(first.id());
            assertThat(enlarged.quantity()).isEqualTo(Stock.of(5));
            // The instant of the original claim survives: it is one reservation that grew.
            assertThat(enlarged.reservedAt()).isEqualTo(NOW);
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(5));
        }

        @Test
        @DisplayName("releasing frees the units and leaves the shelf untouched")
        void releases() {
            InventoryItem item = item(10);
            item.reserve(ORDER, Stock.of(3), CLOCK);

            Reservation released = item.releaseReservation(ORDER, CLOCK);

            assertThat(released.quantity()).isEqualTo(Stock.of(3));
            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(10));
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(10));
            assertThat(item.getReservations()).isEmpty();
        }

        @Test
        @DisplayName("withdrawing takes exactly what the order reserved off the shelf")
        void withdraws() {
            InventoryItem item = item(10);
            item.reserve(ORDER, Stock.of(3), CLOCK);

            StockWithdrawal withdrawal = item.withdraw(ORDER, CLOCK);

            assertThat(withdrawal.serviceOrderId()).isEqualTo(ORDER);
            assertThat(withdrawal.quantity()).isEqualTo(Stock.of(3));
            assertThat(withdrawal.inventoryItemId()).isEqualTo(item.getId());
            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(7));
            assertThat(item.stockReserved()).isEqualTo(Stock.ZERO);
            assertThat(item.getReservations()).isEmpty();
        }

        @Test
        @DisplayName("no material leaves without a reservation behind it — the invariant of the slice")
        void refusesToWithdrawWithoutAReservation() {
            InventoryItem item = item(10);

            assertThatThrownBy(() -> item.withdraw(ORDER, CLOCK)).isInstanceOf(ReservationNotFoundException.class);
            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(10));
        }

        @Test
        void refusesToReleaseWhatNoOrderHolds() {
            assertThatThrownBy(() -> item(10).releaseReservation(ORDER, CLOCK))
                    .isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("a reservation withdrawn twice is refused: the second time there is nothing left")
        void refusesToWithdrawTwice() {
            InventoryItem item = item(10);
            item.reserve(ORDER, Stock.of(3), CLOCK);
            item.withdraw(ORDER, CLOCK);

            assertThatThrownBy(() -> item.withdraw(ORDER, CLOCK)).isInstanceOf(ReservationNotFoundException.class);
            assertThat(item.getStockOnHand()).isEqualTo(Stock.of(7));
        }
    }

    @Nested
    @DisplayName("catalogue")
    class Catalogue {

        @Test
        void correctsNameAndPrice() {
            InventoryItem item = item(1);
            Clock later = Clock.fixed(NOW.plus(Duration.ofDays(1)), ZoneOffset.UTC);

            item.update("Filtro de óleo sintético", new BigDecimal("54.90"), later);

            assertThat(item.getName()).isEqualTo("Filtro de óleo sintético");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("54.90");
            assertThat(item.getUpdatedAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
            assertThat(item.getRegisteredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("removing strands nothing: an item with open reservations is refused")
        void refusesToRemoveWithOpenReservations() {
            InventoryItem item = item(10);
            item.reserve(ORDER, Stock.of(1), CLOCK);

            assertThatThrownBy(() -> item.remove(CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open reservations");
            assertThat(item.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("a removed item takes no operation at all")
        void refusesEveryOperationAfterRemoval() {
            InventoryItem item = item(10);
            item.remove(CLOCK);

            assertThat(item.isRemoved()).isTrue();
            assertThat(item.getRemovedAt()).contains(NOW);
            assertThatThrownBy(() -> item.replenish(Stock.of(1), CLOCK)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> item.reserve(ORDER, Stock.of(1), CLOCK)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> item.update("Correia", BigDecimal.ONE, CLOCK))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> item.remove(CLOCK)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("rehydration")
    class Rehydration {

        @Test
        @DisplayName("rebuilds an item with its open reservations, applying no rule")
        void restores() {
            UUID id = UUID.randomUUID();
            InventoryItem item = InventoryItem.builder()
                    .id(id)
                    .name("Correia dentada")
                    .type(MaterialType.PART)
                    .unitPrice(new BigDecimal("120.00"))
                    .stockOnHand(Stock.of(8))
                    .reservations(List.of(new Reservation(UUID.randomUUID(), ORDER, Stock.of(3), NOW)))
                    .registeredAt(NOW)
                    .updatedAt(NOW)
                    .restore();

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.stockReserved()).isEqualTo(Stock.of(3));
            assertThat(item.stockAvailable()).isEqualTo(Stock.of(5));
            assertThat(item.reservationFor(ORDER)).isPresent();
        }

        @Test
        @DisplayName("two open claims by one order on one item is a state the aggregate refuses to load")
        void rejectsTwoReservationsForTheSameOrder() {
            assertThatThrownBy(() -> InventoryItem.builder()
                            .id(UUID.randomUUID())
                            .name("Correia dentada")
                            .type(MaterialType.PART)
                            .unitPrice(BigDecimal.ONE)
                            .stockOnHand(Stock.of(8))
                            .reservations(List.of(
                                    new Reservation(UUID.randomUUID(), ORDER, Stock.of(1), NOW),
                                    new Reservation(UUID.randomUUID(), ORDER, Stock.of(2), NOW)))
                            .registeredAt(NOW)
                            .updatedAt(NOW)
                            .restore())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Two open reservations");
        }
    }
}
