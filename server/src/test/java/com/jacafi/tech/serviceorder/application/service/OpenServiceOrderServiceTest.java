package com.jacafi.tech.serviceorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.application.service.InventoryAccessPolicy;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

class OpenServiceOrderServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void savesAnOrderWithFrozenPricesReservesStockAndGeneratesItsInitialEstimate() {
        UUID customerId = UUID.randomUUID();
        Vehicle vehicle =
                Vehicle.register(UUID.randomUUID(), new LicensePlate("ABC1D23"), "Ford", "Ka", 2025, customerId, CLOCK);
        ServiceCatalogItem service =
                ServiceCatalogItem.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK);
        InventoryItem material = InventoryItem.register(
                UUID.randomUUID(), "Engine oil", MaterialType.SUPPLY, new BigDecimal("39.90"), Stock.of(5), CLOCK);
        Orders orders = new Orders();
        Inventory inventory = new Inventory(material);
        Trail trail = new Trail();
        CurrentAuthenticatedUserPort user =
                () -> new AuthenticatedUser(UUID.randomUUID().toString(), "advisor", Set.of(Role.EMPLOYEE), customerId);
        ReserveInventoryStockService reserve =
                new ReserveInventoryStockService(inventory, entry -> {}, trail, new InventoryAccessPolicy(user), CLOCK);
        OpenServiceOrderService serviceOrder = new OpenServiceOrderService(
                orders,
                new Vehicles(vehicle),
                new Catalog(service),
                inventory,
                reserve,
                notifications(),
                trail,
                new ServiceOrderAccessPolicy(user),
                CLOCK);

        ServiceOrder opened = serviceOrder.open(new OpenServiceOrderCommand(
                customerId,
                vehicle.id(),
                "Engine noise",
                List.of(new OpenServiceOrderCommand.RequestedService(service.id(), 2)),
                List.of(new OpenServiceOrderCommand.RequestedMaterial(material.id(), 2))));

        assertThat(opened.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(opened.serviceLines())
                .singleElement()
                .extracting(line -> line.quantity())
                .isEqualTo(2);
        assertThat(opened.estimates().getFirst().totalAmount()).isEqualByComparingTo("259.60");
        assertThat(material.stockReserved()).isEqualTo(Stock.of(2));
        assertThat(orders.saved).isSameAs(opened);
        assertThat(trail.events).extracting(AuditEvent::action).contains("OPENED", "RESERVED");
    }

    private static final class Orders implements ServiceOrderRepositoryPort {
        private ServiceOrder saved;

        @Override
        public ServiceOrder save(ServiceOrder order) {
            saved = order;
            return order;
        }

        @Override
        public Optional<ServiceOrder> findById(UUID id) {
            return saved != null && saved.id().equals(id) ? Optional.of(saved) : Optional.empty();
        }

        @Override
        public PageResult<ServiceOrder> findOperationalQueue(PageQuery query) {
            throw new UnsupportedOperationException();
        }
    }

    private record Vehicles(Vehicle vehicle) implements VehicleRepositoryPort {
        @Override
        public Vehicle save(Vehicle ignored, String actor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsActiveByLicensePlate(LicensePlate plate) {
            return false;
        }

        @Override
        public Optional<Vehicle> findActiveById(UUID id) {
            return vehicle.id().equals(id) ? Optional.of(vehicle) : Optional.empty();
        }

        @Override
        public Optional<Vehicle> findActiveByLicensePlate(LicensePlate plate) {
            return Optional.empty();
        }

        @Override
        public PageResult<Vehicle> findActiveByCustomerId(UUID customerId, PageQuery query) {
            throw new UnsupportedOperationException();
        }
    }

    private record Catalog(ServiceCatalogItem item) implements ServiceCatalogRepositoryPort {
        @Override
        public ServiceCatalogItem save(ServiceCatalogItem ignored) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ServiceCatalogItem> findActiveById(UUID id) {
            return item.id().equals(id) ? Optional.of(item) : Optional.empty();
        }

        @Override
        public PageResult<ServiceCatalogItem> findActive(PageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsActiveWithName(String name) {
            return false;
        }

        @Override
        public boolean existsActiveWithNameExcluding(String name, UUID id) {
            return false;
        }
    }

    private static final class Inventory implements InventoryItemRepositoryPort {
        private final InventoryItem item;

        private Inventory(InventoryItem item) {
            this.item = item;
        }

        @Override
        public InventoryItem save(InventoryItem ignored, String actor) {
            return item;
        }

        @Override
        public Optional<InventoryItem> findActiveById(UUID id) {
            return item.id().equals(id) ? Optional.of(item) : Optional.empty();
        }

        @Override
        public Optional<InventoryItem> findActiveByIdForUpdate(UUID id) {
            return findActiveById(id);
        }

        @Override
        public boolean existsActiveWithName(String name) {
            return false;
        }

        @Override
        public boolean existsActiveWithNameExcluding(String name, UUID id) {
            return false;
        }
    }

    private static final class Trail implements AuditTrailPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }

    private static StatusNotificationPort notifications() {
        return (serviceOrderId, customerId, status) -> {};
    }
}
