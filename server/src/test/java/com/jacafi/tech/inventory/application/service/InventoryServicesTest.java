package com.jacafi.tech.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepository;
import com.jacafi.tech.inventory.application.port.InventoryQueryPort;
import com.jacafi.tech.inventory.domain.entity.AuditedOperation;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.inventory.domain.exception.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.domain.ErrorCode;
import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

class InventoryServicesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void operationalRegistrationWritesBothAuditRecordsOnlyAfterSave() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        Trail trail = new Trail();
        InventoryItem item = new RegisterInventoryItemService(items, ledger, trail, operational(), CLOCK)
                .register("Oil filter", MaterialType.PART, new BigDecimal("20.00"), 2);
        assertThat(item.name()).isEqualTo("Oil filter");
        assertThat(ledger.entries)
                .singleElement()
                .extracting(InventoryAuditEntry::operation)
                .isEqualTo(com.jacafi.tech.inventory.domain.entity.AuditedOperation.REGISTERED);
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REGISTERED");
        items.fail = true;
        assertThatThrownBy(() -> new RegisterInventoryItemService(items, ledger, trail, operational(), CLOCK)
                        .register("Air filter", MaterialType.PART, new BigDecimal("20.00"), 2))
                .isInstanceOf(IllegalStateException.class);
        assertThat(ledger.entries).hasSize(1);
        assertThat(trail.events).hasSize(1);
    }

    @Test
    void customerCannotUseInventoryCommands() {
        Items items = new Items();
        assertThatThrownBy(() -> new RegisterInventoryItemService(items, new Ledger(), new Trail(), customer(), CLOCK)
                        .register("Oil filter", MaterialType.PART, BigDecimal.ONE, 1))
                .isInstanceOf(AccountAccessDeniedException.class);
        assertThat(items.storage).isEmpty();
    }

    @Test
    void reserveReleaseAndWithdrawWriteMovementLedger() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        Trail trail = new Trail();
        InventoryItem item = InventoryItem.register(
                UUID.randomUUID(), "Bolt", MaterialType.PART, BigDecimal.ONE, Stock.of(3), CLOCK);
        items.save(item, "employee");
        UUID order = UUID.randomUUID();
        new ReserveInventoryStockService(items, ledger, trail, operational(), CLOCK).reserve(item.id(), order, 2);
        new ReleaseInventoryReservationService(items, ledger, trail, operational(), CLOCK).release(item.id(), order);
        new ReserveInventoryStockService(items, ledger, trail, operational(), CLOCK).reserve(item.id(), order, 2);
        new WithdrawInventoryStockService(items, ledger, trail, operational(), CLOCK).withdraw(item.id(), order);
        assertThat(ledger.entries)
                .extracting(InventoryAuditEntry::operation)
                .containsExactly(
                        com.jacafi.tech.inventory.domain.entity.AuditedOperation.RESERVED,
                        com.jacafi.tech.inventory.domain.entity.AuditedOperation.RELEASED,
                        com.jacafi.tech.inventory.domain.entity.AuditedOperation.RESERVED,
                        com.jacafi.tech.inventory.domain.entity.AuditedOperation.WITHDRAWN);
        assertThat(item.stockOnHand()).isEqualTo(Stock.of(1));
    }

    @Test
    void employeeFindsAndListsActiveItems() {
        Items items = new Items();
        InventoryItem item = items.save(bolt(), "employee");
        InventoryQueryPort queries = (type, query) -> new InventoryPage(List.of(item), query.page(), query.size(), 1);

        assertThat(new FindInventoryItemService(items, operational()).findById(item.id()))
                .isSameAs(item);
        PageResult<InventoryItem> page = new ListInventoryItemsService(queries, operational())
                .list(MaterialType.PART, new PageQuery(0, 20, List.of(SortCriterion.ascending("id"))))
                .toPageResult();
        assertThat(page.content()).containsExactly(item);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void updateRenamesTheItemAndWritesBothAuditRecords() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        Trail trail = new Trail();
        InventoryItem item = items.save(bolt(), "employee");

        InventoryItem updated = new UpdateInventoryItemService(items, ledger, trail, operational(), CLOCK)
                .update(item.id(), "Hex bolt", new BigDecimal("2.50"));

        assertThat(updated.name()).isEqualTo("Hex bolt");
        assertThat(updated.unitPrice()).isEqualByComparingTo("2.50");
        assertThat(ledger.entries)
                .singleElement()
                .extracting(InventoryAuditEntry::operation)
                .isEqualTo(AuditedOperation.UPDATED);
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("UPDATED");
    }

    @Test
    void updateRejectsANameUsedByAnotherActiveItem() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        items.save(
                InventoryItem.register(UUID.randomUUID(), "Nut", MaterialType.PART, BigDecimal.ONE, Stock.of(1), CLOCK),
                "employee");
        InventoryItem bolt = items.save(bolt(), "employee");

        assertThatThrownBy(() -> new UpdateInventoryItemService(items, ledger, new Trail(), operational(), CLOCK)
                        .update(bolt.id(), "nut", BigDecimal.ONE))
                .isInstanceOf(DuplicateMaterialException.class);
        assertThat(ledger.entries).isEmpty();
    }

    @Test
    void replenishAddsStockAndRecordsTheQuantity() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        Trail trail = new Trail();
        InventoryItem item = items.save(bolt(), "employee");

        new ReplenishInventoryStockService(items, ledger, trail, operational(), CLOCK).replenish(item.id(), 2);

        assertThat(item.stockOnHand()).isEqualTo(Stock.of(5));
        assertThat(ledger.entries)
                .singleElement()
                .extracting(InventoryAuditEntry::quantity)
                .isEqualTo(Stock.of(2));
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REPLENISHED");
    }

    @Test
    void removedItemIsNoLongerFoundAndBothAuditRecordsAreWritten() {
        Items items = new Items();
        Ledger ledger = new Ledger();
        Trail trail = new Trail();
        InventoryItem item = items.save(bolt(), "employee");

        new RemoveInventoryItemService(items, ledger, trail, operational(), CLOCK).remove(item.id());

        assertThat(items.findActiveById(item.id())).isEmpty();
        assertThat(ledger.entries)
                .singleElement()
                .extracting(InventoryAuditEntry::operation)
                .isEqualTo(AuditedOperation.REMOVED);
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REMOVED");
    }

    @Test
    void unknownItemIsReportedAsNotFound() {
        assertThatThrownBy(() -> new FindInventoryItemService(new Items(), operational()).findById(UUID.randomUUID()))
                .isInstanceOfSatisfying(
                        InventoryItemNotFoundException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }

    private static InventoryItem bolt() {
        return InventoryItem.register(UUID.randomUUID(), "Bolt", MaterialType.PART, BigDecimal.ONE, Stock.of(3), CLOCK);
    }

    private static InventoryAccessPolicy operational() {
        return new InventoryAccessPolicy(user("employee", Set.of(Role.EMPLOYEE)));
    }

    private static InventoryAccessPolicy customer() {
        return new InventoryAccessPolicy(user("customer", Set.of(Role.CUSTOMER)));
    }

    private static CurrentAuthenticatedUserPort user(String name, Set<Role> roles) {
        return () -> new AuthenticatedUser(UUID.randomUUID().toString(), name, roles, null);
    }

    private static final class Ledger implements InventoryAuditLedgerPort {
        private final List<InventoryAuditEntry> entries = new ArrayList<>();

        public void append(InventoryAuditEntry entry) {
            entries.add(entry);
        }
    }

    private static final class Trail implements AuditTrailPort {
        private final List<AuditEvent> events = new ArrayList<>();

        public void record(AuditEvent event) {
            events.add(event);
        }
    }

    private static final class Items implements InventoryItemRepository {
        private final Map<UUID, InventoryItem> storage = new LinkedHashMap<>();
        private boolean fail;

        public InventoryItem save(InventoryItem item, String actor) {
            if (fail) throw new IllegalStateException("database unavailable");
            storage.put(item.id(), item);
            return item;
        }

        public Optional<InventoryItem> findActiveById(UUID id) {
            return Optional.ofNullable(storage.get(id)).filter(InventoryItem::active);
        }

        public Optional<InventoryItem> findActiveByIdForUpdate(UUID id) {
            return findActiveById(id);
        }

        public boolean existsActiveWithName(String name) {
            return storage.values().stream()
                    .anyMatch(item -> item.active() && item.name().equalsIgnoreCase(name));
        }

        public boolean existsActiveWithNameExcluding(String name, UUID id) {
            return storage.values().stream()
                    .anyMatch(item -> item.active()
                            && !item.id().equals(id)
                            && item.name().equalsIgnoreCase(name));
        }
    }
}
