package com.jacafi.tech.auth.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class AccountAccessDeniedException extends BusinessException {
    public AccountAccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
