package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.domain.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.MaterialType;
import com.jacafi.tech.inventory.domain.Stock;
import com.jacafi.tech.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Exercises the schema directly, bypassing the application layer's checks.
 *
 * <p>This is one of the reasons the integration tests need a real Postgres. H2 implements neither
 * a unique index with a predicate nor one over an expression, so on H2 the uniqueness test here
 * would either not apply at all or would fail for the wrong reason.
 *
 * <p>It also covers what only a round trip can show: that saving an aggregate reconciles its
 * reservation rows — inserting, updating and deleting them — without a cascade doing it by
 * accident.
 */
class InventoryItemRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private InventoryItemRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * The lock-taking finder is only meaningful inside a transaction — a row held until the
     * transaction ends means nothing when there is no transaction to end. Calling it bare fails
     * with "No active transaction", which is the port telling the truth: production callers are
     * the use cases, and every one of them is {@code @Transactional}. This template puts the test
     * on the same footing instead of weakening the query to accommodate it.
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE inventory_reservations, inventory_audit_entries, inventory_items");
    }

    private InventoryItem itemNamed(String name, int onHand) {
        return InventoryItem.builder()
                .id(UUID.randomUUID())
                .name(name)
                .type(MaterialType.PART)
                .unitPrice(new BigDecimal("49.90"))
                .stockOnHand(Stock.of(onHand))
                .register(CLOCK);
    }

    @Test
    @DisplayName("the database refuses a second active material with the same name, case aside")
    void enforcesUniquenessAmongActiveItems() {
        repository.save(itemNamed("Filtro de óleo", 1));

        // Straight at the adapter, so the application layer's check is not in the way: this is the
        // concurrent case, where two registrations both passed that check.
        assertThatExceptionOfType(DuplicateMaterialException.class)
                .isThrownBy(() -> repository.save(itemNamed("FILTRO DE ÓLEO", 1)));
    }

    @Test
    @DisplayName("the name is free again once the previous material was removed")
    void releasesTheNameAfterRemoval() {
        InventoryItem first = itemNamed("Correia dentada", 1);
        repository.save(first);

        first.remove(CLOCK);
        repository.save(first);

        InventoryItem second = itemNamed("Correia dentada", 4);
        repository.save(second);

        assertThat(repository.findActiveById(second.getId())).isPresent();
        assertThat(repository.findActiveById(first.getId())).isEmpty();
        assertThat(repository.existsActiveWithName("Correia dentada")).isTrue();
    }

    @Test
    @DisplayName("a saved aggregate comes back with its balance and its reservations intact")
    void roundTripsTheAggregate() {
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();

        InventoryItem saved = itemNamed("Filtro de óleo", 10);
        saved.reserve(orderA, Stock.of(3), CLOCK);
        saved.reserve(orderB, Stock.of(2), CLOCK);
        repository.save(saved);

        InventoryItem loaded = repository.findActiveById(saved.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Filtro de óleo");
        assertThat(loaded.getType()).isEqualTo(MaterialType.PART);
        assertThat(loaded.getUnitPrice()).isEqualByComparingTo("49.90");
        assertThat(loaded.getStockOnHand()).isEqualTo(Stock.of(10));
        assertThat(loaded.stockReserved()).isEqualTo(Stock.of(5));
        assertThat(loaded.stockAvailable()).isEqualTo(Stock.of(5));
        assertThat(loaded.reservationFor(orderA)).isPresent();
        assertThat(loaded.reservationFor(orderB)).isPresent();
        assertThat(loaded.getRegisteredAt()).isEqualTo(saved.getRegisteredAt());
    }

    @Test
    @DisplayName("saving reconciles the reservation rows: enlarged updates, settled disappears")
    void reconcilesReservationRows() {
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();

        InventoryItem item = itemNamed("Filtro de óleo", 10);
        item.reserve(orderA, Stock.of(3), CLOCK);
        item.reserve(orderB, Stock.of(2), CLOCK);
        repository.save(item);

        assertThat(countReservations(item)).isEqualTo(2);

        transactionTemplate.executeWithoutResult(status -> {
            InventoryItem loaded = repository.findActiveByIdForUpdate(item.getId()).orElseThrow();
            loaded.reserve(orderA, Stock.of(1), CLOCK);
            loaded.withdraw(orderB, CLOCK);
            repository.save(loaded);
        });

        assertThat(countReservations(item)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory_reservations WHERE service_order_id = ?",
                Integer.class, orderA)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock_on_hand FROM inventory_items WHERE id = ?",
                Integer.class, item.getId())).isEqualTo(8);
    }

    @Test
    @DisplayName("releasing every reservation leaves no row behind")
    void deletesTheLastReservationRow() {
        UUID order = UUID.randomUUID();

        InventoryItem item = itemNamed("Filtro de óleo", 10);
        item.reserve(order, Stock.of(3), CLOCK);
        repository.save(item);

        transactionTemplate.executeWithoutResult(status -> {
            InventoryItem loaded = repository.findActiveByIdForUpdate(item.getId()).orElseThrow();
            loaded.releaseReservation(order, CLOCK);
            repository.save(loaded);
        });

        assertThat(countReservations(item)).isZero();
        assertThat(repository.findActiveById(item.getId()).orElseThrow().stockAvailable())
                .isEqualTo(Stock.of(10));
    }

    @Test
    @DisplayName("a removed item answers no lookup and does not hold its name")
    void removedItemsAnswerNoLookup() {
        InventoryItem item = itemNamed("Correia dentada", 2);
        repository.save(item);
        item.remove(CLOCK);
        repository.save(item);

        assertThat(repository.findActiveById(item.getId())).isEmpty();
        transactionTemplate.executeWithoutResult(status ->
                assertThat(repository.findActiveByIdForUpdate(item.getId())).isEmpty());
        assertThat(repository.existsActiveWithName("Correia dentada")).isFalse();
    }

    private Long countReservations(InventoryItem item) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_reservations WHERE inventory_item_id = ?",
                Long.class, item.getId());
    }
}
