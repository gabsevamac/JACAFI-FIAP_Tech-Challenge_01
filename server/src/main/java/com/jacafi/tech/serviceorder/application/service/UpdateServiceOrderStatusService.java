package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class UpdateServiceOrderStatusService {
    private final ServiceOrderRepositoryPort orders;
    private final StatusNotificationPort notifications;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public UpdateServiceOrderStatusService(
            ServiceOrderRepositoryPort orders,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        this.orders = orders;
        this.notifications = notifications;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public ServiceOrder update(UUID serviceOrderId, ServiceOrderStatus status) {
        access.requireEmployee();
        ServiceOrder order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        String actor = access.currentActor();
        ServiceOrderStatus previousStatus = order.status();
        applyTransition(order, status, actor);
        orders.save(order);
        notifications.notifyStatusChanged(order.id(), order.customerId(), status);
        auditTrail.record(new AuditEvent(
                "ServiceOrder",
                order.id(),
                "STATUS_UPDATED",
                actor,
                clock.instant(),
                Map.of("status", previousStatus.name()),
                Map.of("status", status.name())));
        return order;
    }

    private void applyTransition(ServiceOrder order, ServiceOrderStatus status, String actor) {
        switch (status) {
            case UNDER_DIAGNOSIS -> order.startDiagnosis(actor, clock);
            case COMPLETED -> order.complete(actor, clock);
            case DELIVERED -> order.deliver(actor, clock);
            default -> throw new IllegalArgumentException("This service order status cannot be set directly");
        }
    }
}
