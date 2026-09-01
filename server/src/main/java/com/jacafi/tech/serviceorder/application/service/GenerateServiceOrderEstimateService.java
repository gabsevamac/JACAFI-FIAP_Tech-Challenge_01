package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class GenerateServiceOrderEstimateService {
    private final ServiceOrderRepositoryPort orders;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public GenerateServiceOrderEstimateService(
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
    public Estimate generate(UUID serviceOrderId) {
        access.requireEmployee();
        var order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        String actor = access.currentActor();
        Estimate estimate = order.generateEstimate(actor, clock);
        orders.save(order);
        auditTrail.record(new AuditEvent("ServiceOrder", serviceOrderId, "ESTIMATE_GENERATED", actor, clock.instant()));
        return estimate;
    }
}
