package com.jacafi.tech.vehicle.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class VehicleNotFoundException extends BusinessException {

    public VehicleNotFoundException() {
        super(ErrorCode.VEHICLE_NOT_FOUND);
    }
}
