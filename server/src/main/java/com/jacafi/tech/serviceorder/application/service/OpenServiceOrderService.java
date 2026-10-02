package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.application.port.InventoryItemRepository;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.exception.LaborOperationNotFoundException;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.LaborLineItem;
import com.jacafi.tech.serviceorder.domain.entity.MaterialLineItem;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepository;
import com.jacafi.tech.vehicle.domain.exception.VehicleNotFoundException;

public class OpenServiceOrderService {
    private final ServiceOrderRepository orders;
    private final VehicleRepository vehicles;
    private final LaborOperationRepository laborOperations;
    private final InventoryItemRepository inventory;
    private final ReserveInventoryStockService reserveInventory;
    private final StatusNotificationPort notifications;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public OpenServiceOrderService(
            ServiceOrderRepository orders,
            VehicleRepository vehicles,
            LaborOperationRepository laborOperations,
            InventoryItemRepository inventory,
            ReserveInventoryStockService reserveInventory,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        this.orders = orders;
        this.vehicles = vehicles;
        this.laborOperations = laborOperations;
        this.inventory = inventory;
        this.reserveInventory = reserveInventory;
        this.notifications = notifications;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public ServiceOrder open(OpenServiceOrderCommand command) {
        access.requireEmployee();
        var vehicle = vehicles.findActiveById(command.vehicleId()).orElseThrow(VehicleNotFoundException::new);
        if (!vehicle.customerId().equals(command.customerId())) {
            throw new IllegalArgumentException("vehicle must belong to customer");
        }
        List<LaborLineItem> laborLines = laborLines(command.laborOperations());
        List<MaterialLineItem> materials = materialLines(command.materials());
        String actor = access.currentActor();
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(),
                command.customerId(),
                command.vehicleId(),
                command.reportedIssue(),
                laborLines,
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
        notifications.notifyStatusChanged(saved.id(), saved.customerId(), saved.status());
        auditTrail.record(new AuditEvent("ServiceOrder", saved.id(), "OPENED", actor, clock.instant()));
        return saved;
    }

    private List<LaborLineItem> laborLines(List<OpenServiceOrderCommand.RequestedLaborOperation> requested) {
        requireDistinct(requested.stream()
                .map(OpenServiceOrderCommand.RequestedLaborOperation::laborOperationId)
                .toList());
        return requested.stream()
                .map(request -> {
                    var operation = laborOperations
                            .findActiveById(request.laborOperationId())
                            .orElseThrow(LaborOperationNotFoundException::new);
                    return LaborLineItem.of(
                            UUID.randomUUID(),
                            operation.id(),
                            operation.name(),
                            operation.basePrice(),
                            request.quantity());
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
