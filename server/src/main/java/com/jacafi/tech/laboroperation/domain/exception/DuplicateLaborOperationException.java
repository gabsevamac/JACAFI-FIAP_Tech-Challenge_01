package com.jacafi.tech.laboroperation.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class DuplicateLaborOperationException extends BusinessException {
    public DuplicateLaborOperationException() {
        super(ErrorCode.DUPLICATE_LABOR_OPERATION);
    }
}
