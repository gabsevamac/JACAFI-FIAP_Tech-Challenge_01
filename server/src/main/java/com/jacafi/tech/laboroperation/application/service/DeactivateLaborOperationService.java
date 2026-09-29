package com.jacafi.tech.laboroperation.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.exception.LaborOperationNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class DeactivateLaborOperationService {
    private final LaborOperationRepository operations;
    private final AuditTrailPort auditTrail;
    private final LaborOperationAccessPolicy access;
    private final Clock clock;

    public DeactivateLaborOperationService(
            LaborOperationRepository operations,
            AuditTrailPort auditTrail,
            LaborOperationAccessPolicy access,
            Clock clock) {
        this.operations = operations;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public void deactivate(UUID id) {
        access.requireManagementAccess();
        var operation = operations.findActiveById(id).orElseThrow(LaborOperationNotFoundException::new);
        operation.deactivate(clock);
        operations.save(operation);
        auditTrail.record(new AuditEvent("LaborOperation", id, "DEACTIVATED", access.currentActor(), clock.instant()));
    }
}
