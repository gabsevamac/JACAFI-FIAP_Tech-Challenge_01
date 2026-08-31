package com.jacafi.tech.customer.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class CustomerAlreadyExistsException extends BusinessException {

    public CustomerAlreadyExistsException() {
        super(ErrorCode.CUSTOMER_ALREADY_EXISTS);
    }
}
