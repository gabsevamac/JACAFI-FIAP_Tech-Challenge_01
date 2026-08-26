package com.jacafi.tech.customer.exception;

import com.jacafi.tech.shared.web.BusinessException;
import com.jacafi.tech.shared.web.ErrorCode;

public class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException() {
        super(ErrorCode.CUSTOMER_NOT_FOUND);
    }
}
