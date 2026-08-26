package com.jacafi.tech.customer.exception;

import com.jacafi.tech.shared.web.BusinessException;
import com.jacafi.tech.shared.web.ErrorCode;

public class InvalidTaxIdException extends BusinessException {

    public InvalidTaxIdException() {
        super(ErrorCode.INVALID_TAX_ID);
    }
}
