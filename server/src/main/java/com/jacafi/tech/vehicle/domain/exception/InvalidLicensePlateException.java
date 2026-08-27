package com.jacafi.tech.vehicle.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class InvalidLicensePlateException extends BusinessException {

    public InvalidLicensePlateException() {
        super(ErrorCode.INVALID_LICENSE_PLATE);
    }
}
