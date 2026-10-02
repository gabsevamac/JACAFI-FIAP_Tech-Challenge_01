package com.jacafi.tech.laboroperation.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class LaborOperationNotFoundException extends BusinessException {
    public LaborOperationNotFoundException() {
        super(ErrorCode.LABOR_OPERATION_NOT_FOUND);
    }
}
