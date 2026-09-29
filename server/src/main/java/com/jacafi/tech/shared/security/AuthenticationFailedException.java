package com.jacafi.tech.shared.security;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class AuthenticationFailedException extends BusinessException {
    public AuthenticationFailedException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
