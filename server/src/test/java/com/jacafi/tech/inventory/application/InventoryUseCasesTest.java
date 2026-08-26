package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.AuditedOperation;
import com.jacafi.tech.inventory.domain.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.InsufficientStockException;
import com.jacafi.tech.inventory.domain.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.MaterialType;
import com.jacafi.tech.inventory.domain.Quantity;
import com.jacafi.tech.inventory.domain.ReservationNotFoundException;
import com.jacafi.tech.inventory.domain.StockWithdrawal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The use cases against hand-written doubles: no Spring, no database, no clock of its own.
 *
 * <p>What is under test here is what the aggregate cannot decide alone — uniqueness across the
 * catalogue, what gets written to the ledger, and that a mutated aggregate is actually saved.
 */
class InventoryUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String ACTOR = "admin";

    private InMemoryInventoryItemRepository repository;
    private RecordingInventoryAuditTrail auditTrail;

    private RegisterMaterialUseCase registerMaterial;
    private UpdateMaterialUseCase updateMaterial;
    private RemoveMaterialUseCase removeMaterial;
    private ReplenishStockUseCase replenishStock;
    private ReserveMaterialUseCase reserveMaterial;
    private ReleaseReservationUseCase releaseReservation;
    private WithdrawMaterialUseCase withdrawMaterial;
    private FindInventoryItemUseCase findInventoryItem;
    private ListInventoryUseCase listInventory;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInventoryItemRepository();
        auditTrail = new RecordingInventoryAuditTrail();

        registerMaterial = new RegisterMaterialUseCase(repository, auditTrail, CLOCK);
        updateMaterial = new UpdateMaterialUseCase(repository, auditTrail, CLOCK);
        removeMaterial = new RemoveMaterialUseCase(repository, auditTrail, CLOCK);
        replenishStock = new ReplenishStockUseCase(repository, auditTrail, CLOCK);
        reserveMaterial = new ReserveMaterialUseCase(repository, auditTrail, CLOCK);
        releaseReservation = new ReleaseReservationUseCase(repository, auditTrail, CLOCK);
        withdrawMaterial = new WithdrawMaterialUseCase(repository, auditTrail, CLOCK);
        findInventoryItem = new FindInventoryItemUseCase(repository);
        listInventory = new ListInventoryUseCase(repository);
    }

    private InventoryItem register(String name, MaterialType type, int initialQuantity) {
        return registerMaterial.register(
                new RegisterMaterialCommand(name, type, new BigDecimal("49.90"), initialQuantity, ACTOR));
    }

    @Nested
    @DisplayName("catalogue")
    class Catalogue {

        @Test
        @DisplayName("registering stores the item and records who registered it")
        void registers() {
            InventoryItem item = register("Filtro de óleo", MaterialType.PART, 12);

            assertThat(repository.findActiveById(item.getId())).contains(item);

            InventoryAuditEntry entry = auditTrail.only();
            assertThat(entry.inventoryItemId()).isEqualTo(item.getId());
            assertThat(entry.operation()).isEqualTo(AuditedOperation.REGISTERED);
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.occurredAt()).isEqualTo(NOW);
            // A catalogue operation names no order and moves no stock.
            assertThat(entry.optionalServiceOrderId()).isEmpty();
            assertThat(entry.optionalQuantity()).isEmpty();
        }

        @Test
        @DisplayName("the duplicate check runs on the normalized name, not on what was typed")
        void rejectsDuplicateNameRegardlessOfSpacingAndCase() {
            register("Filtro de óleo", MaterialType.PART, 1);

            assertThatThrownBy(() -> register("  filtro   DE óleo ", MaterialType.SUPPLY, 1))
                    .isInstanceOf(DuplicateMaterialException.class);
            assertThat(auditTrail.entries()).hasSize(1);
        }

        @Test
        @DisplayName("a removed item releases its name for a new registration")
        void freesTheNameOnRemoval() {
            InventoryItem item = register("Correia dentada", MaterialType.PART, 1);
            removeMaterial.remove(item.getId(), ACTOR);

            InventoryItem replacement = register("Correia dentada", MaterialType.PART, 1);

            assertThat(replacement.getId()).isNotEqualTo(item.getId());
            assertThat(findInventoryItem.byId(replacement.getId())).isEqualTo(replacement);
            assertThatThrownBy(() -> findInventoryItem.byId(item.getId()))
                    .isInstanceOf(InventoryItemNotFoundException.class);
        }

        @Test
        @DisplayName("renaming an item onto another item's name is refused")
        void rejectsRenameOntoAnotherName() {
            register("Filtro de óleo", MaterialType.PART, 1);
            InventoryItem other = register("Filtro de ar", MaterialType.PART, 1);

            assertThatThrownBy(() -> updateMaterial.update(new UpdateMaterialCommand(
                    other.getId(), "Filtro de óleo", new BigDecimal("10.00"), ACTOR)))
                    .isInstanceOf(DuplicateMaterialException.class);
        }

        @Test
        @DisplayName("an item may keep its own name while its price changes")
        void allowsUpdateKeepingTheSameName() {
            InventoryItem item = register("Filtro de óleo", MaterialType.PART, 1);

            InventoryItem updated = updateMaterial.update(new UpdateMaterialCommand(
                    item.getId(), "Filtro de óleo", new BigDecimal("54.90"), ACTOR));

            assertThat(updated.getUnitPrice()).isEqualByComparingTo("54.90");
            assertThat(auditTrail.last().operation()).isEqualTo(AuditedOperation.UPDATED);
        }

        @Test
        @DisplayName("a removed item is not found, rather than found and refused")
        void removedItemAnswersNothing() {
            InventoryItem item = register("Correia dentada", MaterialType.PART, 1);
            removeMaterial.remove(item.getId(), ACTOR);

            assertThatThrownBy(() -> removeMaterial.remove(item.getId(), ACTOR))
                    .isInstanceOf(InventoryItemNotFoundException.class);
            assertThatThrownBy(() -> replenishStock.replenish(
                    new ReplenishStockCommand(item.getId(), 1, ACTOR)))
                    .isInstanceOf(InventoryItemNotFoundException.class);
        }

        @Test
        void listsActiveItemsByType() {
            register("Filtro de óleo", MaterialType.PART, 1);
            register("Óleo 5W30", MaterialType.SUPPLY, 1);
            InventoryItem removed = register("Correia dentada", MaterialType.PART, 1);
            removeMaterial.remove(removed.getId(), ACTOR);

            assertThat(listInventory.list(null, 0, 10).content()).hasSize(2);
            assertThat(listInventory.list(MaterialType.PART, 0, 10).content())
                    .extracting(InventoryItem::getName)
                    .containsExactly("Filtro de óleo");
        }
    }

    @Nested
    @DisplayName("stock movement")
    class StockMovement {

        private UUID itemId;
        private final UUID serviceOrderId = UUID.randomUUID();

        @BeforeEach
        void registerItem() {
            itemId = register("Filtro de óleo", MaterialType.PART, 10).getId();
        }

        @Test
        @DisplayName("a replenishment is recorded with its quantity and with no order behind it")
        void auditsAReplenishment() {
            replenishStock.replenish(new ReplenishStockCommand(itemId, 5, ACTOR));

            InventoryAuditEntry entry = auditTrail.last();
            assertThat(entry.operation()).isEqualTo(AuditedOperation.REPLENISHED);
            assertThat(entry.optionalQuantity()).contains(Quantity.of(5));
            assertThat(entry.optionalServiceOrderId()).isEmpty();
            assertThat(findInventoryItem.byId(itemId).getQuantityOnHand()).isEqualTo(Quantity.of(15));
        }

        @Test
        @DisplayName("a reservation is recorded against the order that authorized it")
        void auditsAReservation() {
            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 4, ACTOR));

            InventoryAuditEntry entry = auditTrail.last();
            assertThat(entry.operation()).isEqualTo(AuditedOperation.RESERVED);
            assertThat(entry.optionalServiceOrderId()).contains(serviceOrderId);
            assertThat(entry.optionalQuantity()).contains(Quantity.of(4));

            InventoryItem item = findInventoryItem.byId(itemId);
            assertThat(item.getQuantityOnHand()).isEqualTo(Quantity.of(10));
            assertThat(item.quantityAvailable()).isEqualTo(Quantity.of(6));
        }

        @Test
        @DisplayName("the ledger records the units this command moved, not the order's running total")
        void auditsTheIncrementOfAnEnlargedReservation() {
            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 2, ACTOR));
            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 3, ACTOR));

            assertThat(auditTrail.last().optionalQuantity()).contains(Quantity.of(3));
            assertThat(findInventoryItem.byId(itemId).quantityReserved()).isEqualTo(Quantity.of(5));
        }

        @Test
        @DisplayName("asking for more than is available writes nothing at all")
        void refusesAndRecordsNothing() {
            int auditedBefore = auditTrail.entries().size();
            int savedBefore = repository.saveCount();

            assertThatThrownBy(() -> reserveMaterial.reserve(
                    new ReserveMaterialCommand(itemId, serviceOrderId, 11, ACTOR)))
                    .isInstanceOf(InsufficientStockException.class);

            assertThat(auditTrail.entries()).hasSize(auditedBefore);
            assertThat(repository.saveCount()).isEqualTo(savedBefore);
        }

        @Test
        @DisplayName("releasing gives the units back and records what was given back")
        void releases() {
            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 4, ACTOR));

            releaseReservation.release(itemId, serviceOrderId, ACTOR);

            InventoryAuditEntry entry = auditTrail.last();
            assertThat(entry.operation()).isEqualTo(AuditedOperation.RELEASED);
            assertThat(entry.optionalQuantity()).contains(Quantity.of(4));

            InventoryItem item = findInventoryItem.byId(itemId);
            assertThat(item.getQuantityOnHand()).isEqualTo(Quantity.of(10));
            assertThat(item.quantityAvailable()).isEqualTo(Quantity.of(10));
        }

        @Test
        @DisplayName("withdrawing takes the reserved units off the shelf and leaves a ledger line")
        void withdraws() {
            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 4, ACTOR));

            StockWithdrawal withdrawal = withdrawMaterial.withdraw(itemId, serviceOrderId, ACTOR);

            assertThat(withdrawal.quantity()).isEqualTo(Quantity.of(4));
            InventoryAuditEntry entry = auditTrail.last();
            assertThat(entry.operation()).isEqualTo(AuditedOperation.WITHDRAWN);
            assertThat(entry.optionalServiceOrderId()).contains(serviceOrderId);

            InventoryItem item = findInventoryItem.byId(itemId);
            assertThat(item.getQuantityOnHand()).isEqualTo(Quantity.of(6));
            assertThat(item.quantityAvailable()).isEqualTo(Quantity.of(6));
        }

        @Test
        @DisplayName("an order that reserved nothing cannot withdraw anything")
        void refusesToWithdrawWithoutReservation() {
            assertThatThrownBy(() -> withdrawMaterial.withdraw(itemId, serviceOrderId, ACTOR))
                    .isInstanceOf(ReservationNotFoundException.class);
            assertThat(findInventoryItem.byId(itemId).getQuantityOnHand()).isEqualTo(Quantity.of(10));
        }

        @Test
        @DisplayName("every movement saves the aggregate it changed")
        void savesWhatItChanged() {
            int before = repository.saveCount();

            reserveMaterial.reserve(new ReserveMaterialCommand(itemId, serviceOrderId, 1, ACTOR));
            replenishStock.replenish(new ReplenishStockCommand(itemId, 1, ACTOR));
            withdrawMaterial.withdraw(itemId, serviceOrderId, ACTOR);

            assertThat(repository.saveCount()).isEqualTo(before + 3);
        }
    }
}
