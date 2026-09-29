package com.jacafi.tech.laboroperation.application.service;

import java.util.UUID;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.laboroperation.domain.exception.LaborOperationNotFoundException;

public class FindLaborOperationService {
    private final LaborOperationRepository operations;
    private final LaborOperationAccessPolicy access;

    public FindLaborOperationService(LaborOperationRepository operations, LaborOperationAccessPolicy access) {
        this.operations = operations;
        this.access = access;
    }

    public LaborOperation findById(UUID id) {
        access.requireOperationalAccess();
        return operations.findActiveById(id).orElseThrow(LaborOperationNotFoundException::new);
    }
}
