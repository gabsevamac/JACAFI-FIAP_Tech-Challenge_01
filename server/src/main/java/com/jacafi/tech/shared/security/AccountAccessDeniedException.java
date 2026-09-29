package com.jacafi.tech.shared.security;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class AccountAccessDeniedException extends BusinessException {
    public AccountAccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
