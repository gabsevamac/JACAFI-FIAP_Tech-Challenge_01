package com.jacafi.tech.laboroperation.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.laboroperation.domain.exception.DuplicateLaborOperationException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class RegisterLaborOperationService {
    private final LaborOperationRepository operations;
    private final AuditTrailPort auditTrail;
    private final LaborOperationAccessPolicy access;
    private final Clock clock;

    public RegisterLaborOperationService(
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
    public LaborOperation register(String name, String description, BigDecimal basePrice) {
        access.requireManagementAccess();
        LaborOperation operation = LaborOperation.register(UUID.randomUUID(), name, description, basePrice, clock);
        if (operations.existsActiveWithName(operation.name())) {
            throw new DuplicateLaborOperationException();
        }
        LaborOperation saved = operations.save(operation);
        String actor = access.currentActor();
        auditTrail.record(new AuditEvent("LaborOperation", saved.id(), "REGISTERED", actor, clock.instant()));
        return saved;
    }
}
