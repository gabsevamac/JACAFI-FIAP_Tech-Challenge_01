package com.jacafi.tech.laboroperation.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.laboroperation.domain.exception.DuplicateLaborOperationException;
import com.jacafi.tech.laboroperation.domain.exception.LaborOperationNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class UpdateLaborOperationService {
    private final LaborOperationRepository operations;
    private final AuditTrailPort auditTrail;
    private final LaborOperationAccessPolicy access;
    private final Clock clock;

    public UpdateLaborOperationService(
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
    public LaborOperation update(UUID id, String name, String description, BigDecimal basePrice) {
        access.requireManagementAccess();
        LaborOperation operation = operations.findActiveById(id).orElseThrow(LaborOperationNotFoundException::new);
        operation.update(name, description, basePrice, clock);
        if (operations.existsActiveWithNameExcluding(operation.name(), operation.id())) {
            throw new DuplicateLaborOperationException();
        }
        LaborOperation saved = operations.save(operation);
        auditTrail.record(
                new AuditEvent("LaborOperation", saved.id(), "UPDATED", access.currentActor(), clock.instant()));
        return saved;
    }
}
