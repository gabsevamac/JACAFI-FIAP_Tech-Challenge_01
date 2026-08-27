package com.jacafi.tech.customer.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException() {
        super(ErrorCode.CUSTOMER_NOT_FOUND);
    }
}
