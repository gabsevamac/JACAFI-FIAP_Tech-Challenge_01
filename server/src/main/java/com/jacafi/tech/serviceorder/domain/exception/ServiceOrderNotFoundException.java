package com.jacafi.tech.serviceorder.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class ServiceOrderNotFoundException extends BusinessException {
    public ServiceOrderNotFoundException() {
        super(ErrorCode.SERVICE_ORDER_NOT_FOUND);
    }
}
