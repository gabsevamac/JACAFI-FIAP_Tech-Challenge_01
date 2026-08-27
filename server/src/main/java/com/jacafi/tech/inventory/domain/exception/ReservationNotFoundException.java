package com.jacafi.tech.inventory.domain.exception;

import java.util.UUID;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class ReservationNotFoundException extends BusinessException {
    public ReservationNotFoundException(UUID inventoryItemId, UUID serviceOrderId) {
        super(ErrorCode.RESERVATION_NOT_FOUND);
    }
}
