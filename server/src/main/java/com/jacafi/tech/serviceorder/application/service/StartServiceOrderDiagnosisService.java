package com.jacafi.tech.serviceorder.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.exception.ServiceOrderNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class StartServiceOrderDiagnosisService {
    private final ServiceOrderRepositoryPort orders;
    private final AuditTrailPort auditTrail;
    private final ServiceOrderAccessPolicy access;
    private final Clock clock;

    public StartServiceOrderDiagnosisService(
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
    public void start(UUID serviceOrderId) {
        access.requireEmployee();
        var order = orders.findById(serviceOrderId).orElseThrow(ServiceOrderNotFoundException::new);
        String actor = access.currentActor();
        order.startDiagnosis(actor, clock);
        orders.save(order);
        auditTrail.record(new AuditEvent("ServiceOrder", serviceOrderId, "DIAGNOSIS_STARTED", actor, clock.instant()));
    }
}
