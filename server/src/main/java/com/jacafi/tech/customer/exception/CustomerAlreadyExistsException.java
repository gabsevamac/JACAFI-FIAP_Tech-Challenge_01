package com.jacafi.tech.customer.exception;

import com.jacafi.tech.shared.web.BusinessException;
import com.jacafi.tech.shared.web.ErrorCode;

public class CustomerAlreadyExistsException extends BusinessException {

    public CustomerAlreadyExistsException() {
        super(ErrorCode.CUSTOMER_ALREADY_EXISTS);
    }
}
