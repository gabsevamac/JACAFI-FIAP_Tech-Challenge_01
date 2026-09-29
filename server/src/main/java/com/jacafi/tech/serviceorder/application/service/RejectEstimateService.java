package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class RejectEstimateService {
    private final ServiceOrderRepository orders;
    private final StatusNotificationPort notifications;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public RejectEstimateService(
            ServiceOrderRepository orders,
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
    public Estimate reject(UUID serviceOrderId, UUID estimateId, String idempotencyKey) {
        var order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        access.requireReadAccess(order.customerId());
        String actor = access.currentActor();
        Estimate estimate = order.rejectEstimate(estimateId, idempotencyKey, actor, clock);
        orders.save(order);
        notifications.notifyStatusChanged(order.id(), order.customerId(), order.status());
        auditTrail.record(new AuditEvent("ServiceOrder", serviceOrderId, "ESTIMATE_REJECTED", actor, clock.instant()));
        return estimate;
    }
}
