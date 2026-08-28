package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.EstimateDecision;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class DecideEstimateService {
    private final ServiceOrderRepositoryPort orders;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public DecideEstimateService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        this.orders = orders;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public Estimate decide(UUID serviceOrderId, UUID estimateId, EstimateDecision decision, String idempotencyKey) {
        access.requireOperationalAccess();
        var order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        String actor = access.currentActor();
        Estimate estimate = order.decideEstimate(estimateId, decision, idempotencyKey, actor, clock);
        orders.save(order);
        auditTrail.record(new AuditEvent(
                "ServiceOrder",
                serviceOrderId,
                decision == EstimateDecision.APPROVE ? "ESTIMATE_APPROVED" : "ESTIMATE_REJECTED",
                actor,
                clock.instant()));
        return estimate;
    }
}
