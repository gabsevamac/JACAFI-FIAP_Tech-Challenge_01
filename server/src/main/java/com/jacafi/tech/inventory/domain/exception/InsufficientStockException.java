package com.jacafi.tech.inventory.domain.exception;

import com.jacafi.tech.inventory.domain.entity.Stock;
import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class InsufficientStockException extends BusinessException {
    public InsufficientStockException(Stock requested, Stock available) {
        super(ErrorCode.INSUFFICIENT_STOCK);
    }
}
