package com.jacafi.tech.inventory.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class DuplicateMaterialException extends BusinessException {
    public DuplicateMaterialException() {
        super(ErrorCode.DUPLICATE_MATERIAL);
    }
}
