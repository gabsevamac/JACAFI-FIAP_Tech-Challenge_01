package com.jacafi.tech.vehicle.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class DuplicateLicensePlateException extends BusinessException {

    public DuplicateLicensePlateException() {
        super(ErrorCode.DUPLICATE_LICENSE_PLATE);
    }
}
