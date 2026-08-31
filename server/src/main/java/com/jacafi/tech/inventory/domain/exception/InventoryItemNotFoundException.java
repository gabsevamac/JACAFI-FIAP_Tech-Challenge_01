package com.jacafi.tech.inventory.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class InventoryItemNotFoundException extends BusinessException {
    public InventoryItemNotFoundException() {
        super(ErrorCode.INVENTORY_ITEM_NOT_FOUND);
    }
}
