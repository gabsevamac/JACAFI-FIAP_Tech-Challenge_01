package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.exception.ServiceCatalogItemNotFoundException;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.MaterialLineItem;
import com.jacafi.tech.serviceorder.domain.entity.ServiceLineItem;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.exception.VehicleNotFoundException;

public class OpenServiceOrderService {
    private final ServiceOrderRepositoryPort orders;
    private final VehicleRepositoryPort vehicles;
    private final ServiceCatalogRepositoryPort catalog;
    private final InventoryItemRepositoryPort inventory;
    private final ReserveInventoryStockService reserveInventory;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public OpenServiceOrderService(
            ServiceOrderRepositoryPort orders,
            VehicleRepositoryPort vehicles,
            ServiceCatalogRepositoryPort catalog,
            InventoryItemRepositoryPort inventory,
            ReserveInventoryStockService reserveInventory,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        this.orders = orders;
        this.vehicles = vehicles;
        this.catalog = catalog;
        this.inventory = inventory;
        this.reserveInventory = reserveInventory;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public ServiceOrder open(OpenServiceOrderCommand command) {
        access.requireOperationalAccess();
        var vehicle = vehicles.findActiveById(command.vehicleId()).orElseThrow(VehicleNotFoundException::new);
        if (!vehicle.customerId().equals(command.customerId())) {
            throw new IllegalArgumentException("vehicle must belong to customer");
        }
        List<ServiceLineItem> services = serviceLines(command.services());
        List<MaterialLineItem> materials = materialLines(command.materials());
        String actor = access.currentActor();
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(),
                command.customerId(),
                command.vehicleId(),
                command.reportedIssue(),
                services,
                materials,
                actor,
                clock);
        ServiceOrder saved = orders.save(order);
        for (MaterialLineItem material : materials) {
            reserveInventory.reserve(material.inventoryItemId(), saved.id(), material.quantity());
        }
        order.startDiagnosis(actor, clock);
        order.generateEstimate(actor, clock);
        saved = orders.save(order);
        auditTrail.record(new AuditEvent("ServiceOrder", saved.id(), "OPENED", actor, clock.instant()));
        return saved;
    }

    private List<ServiceLineItem> serviceLines(List<OpenServiceOrderCommand.RequestedService> requested) {
        requireDistinct(requested.stream()
                .map(OpenServiceOrderCommand.RequestedService::serviceCatalogItemId)
                .toList());
        return requested.stream()
                .map(request -> {
                    var item = catalog.findActiveById(request.serviceCatalogItemId())
                            .orElseThrow(ServiceCatalogItemNotFoundException::new);
                    return ServiceLineItem.of(
                            UUID.randomUUID(), item.id(), item.name(), item.basePrice(), request.quantity());
                })
                .toList();
    }

    private List<MaterialLineItem> materialLines(List<OpenServiceOrderCommand.RequestedMaterial> requested) {
        requireDistinct(requested.stream()
                .map(OpenServiceOrderCommand.RequestedMaterial::inventoryItemId)
                .toList());
        return requested.stream()
                .map(item -> new RequestedInventoryItem(
                        inventory
                                .findActiveById(item.inventoryItemId())
                                .orElseThrow(InventoryItemNotFoundException::new),
                        item.quantity()))
                .map(item -> MaterialLineItem.of(
                        UUID.randomUUID(),
                        item.item().id(),
                        item.item().name(),
                        item.item().unitPrice(),
                        item.quantity()))
                .toList();
    }

    private static void requireDistinct(List<UUID> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("A service order may contain each item only once");
        }
    }

    private record RequestedInventoryItem(InventoryItem item, int quantity) {}
}
