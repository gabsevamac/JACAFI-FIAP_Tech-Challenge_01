package com.jacafi.tech.customer.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class CustomerUpdateConflictException extends BusinessException {

    public CustomerUpdateConflictException() {
        super(ErrorCode.DATA_CONFLICT);
    }
}
