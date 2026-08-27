package com.jacafi.tech.vehicle.domain;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

/**
 * The collection was asked for by neither key, or by both at once.
 *
 * <p>A plate identifies exactly one vehicle and a customer owns a list of them, so the two
 * parameters yield different response shapes. Guessing which one the caller meant would make the
 * response type depend on a coin flip.
 */
public class AmbiguousVehicleQueryException extends BusinessException {

    public AmbiguousVehicleQueryException() {
        super(ErrorCode.VEHICLE_QUERY_AMBIGUOUS);
    }
}
