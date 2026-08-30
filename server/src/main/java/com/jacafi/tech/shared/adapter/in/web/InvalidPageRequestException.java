package com.jacafi.tech.shared.adapter.in.web;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public class InvalidPageRequestException extends BusinessException {

    public InvalidPageRequestException(String message) {
        super(ErrorCode.INVALID_PAGING, message, null);
    }
}
