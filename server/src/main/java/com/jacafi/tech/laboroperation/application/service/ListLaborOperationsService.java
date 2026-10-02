package com.jacafi.tech.laboroperation.application.service;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public class ListLaborOperationsService {
    private final LaborOperationRepository operations;
    private final LaborOperationAccessPolicy access;

    public ListLaborOperationsService(LaborOperationRepository operations, LaborOperationAccessPolicy access) {
        this.operations = operations;
        this.access = access;
    }

    public PageResult<LaborOperation> list(PageQuery query) {
        access.requireEmployee();
        return operations.findActive(query);
    }
}
