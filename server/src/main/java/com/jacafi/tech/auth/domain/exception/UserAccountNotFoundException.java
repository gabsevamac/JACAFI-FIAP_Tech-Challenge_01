package com.jacafi.tech.auth.domain.exception;

import java.util.UUID;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class UserAccountNotFoundException extends BusinessException {
    public UserAccountNotFoundException(UUID id) {
        super(ErrorCode.USER_ACCOUNT_NOT_FOUND, id.toString());
    }
}
