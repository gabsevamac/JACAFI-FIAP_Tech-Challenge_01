package com.jacafi.tech.vehicle.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class VehicleUpdateConflictException extends BusinessException {

    public VehicleUpdateConflictException() {
        super(ErrorCode.DATA_CONFLICT);
    }
}
