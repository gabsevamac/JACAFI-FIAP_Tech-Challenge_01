package com.jacafi.tech.customer.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public class InvalidTaxIdException extends BusinessException {

    public InvalidTaxIdException() {
        super(ErrorCode.INVALID_TAX_ID);
    }
}
