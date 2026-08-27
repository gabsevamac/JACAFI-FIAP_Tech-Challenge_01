package com.jacafi.tech.vehicle.domain;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

/**
 * A license plate was rejected for not matching either accepted Brazilian format.
 *
 * <p>Extends {@link IllegalArgumentException} because that is what it is: the caller supplied an
 * argument the domain cannot accept. The message never carries the offending value.
 */
public class InvalidLicensePlateException extends BusinessException {

    public InvalidLicensePlateException(String message) {
        super(ErrorCode.INVALID_LICENSE_PLATE);
    }
}
