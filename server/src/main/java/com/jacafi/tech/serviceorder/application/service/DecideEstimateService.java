package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.EstimateDecision;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class DecideEstimateService {
    private final ServiceOrderRepositoryPort orders;
    private final StatusNotificationPort notifications;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public DecideEstimateService(
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
    public Estimate decide(UUID serviceOrderId, UUID estimateId, EstimateDecision decision, String idempotencyKey) {
        var order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        access.requireReadAccess(order.customerId());
        String actor = access.currentActor();
        Estimate estimate = order.decideEstimate(estimateId, decision, idempotencyKey, actor, clock);
        orders.save(order);
        notifications.notifyStatusChanged(order.id(), order.customerId(), order.status());
        auditTrail.record(new AuditEvent(
                "ServiceOrder",
                serviceOrderId,
                decision == EstimateDecision.APPROVE ? "ESTIMATE_APPROVED" : "ESTIMATE_REJECTED",
                actor,
                clock.instant()));
        return estimate;
    }
}
