package com.jacafi.tech.shared.domain;

import java.util.Objects;

public abstract class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final String logContext;

    protected BusinessException(ErrorCode errorCode) {
        this(errorCode, (String) null);
    }

    protected BusinessException(ErrorCode errorCode, String logContext) {
        super(errorCode.message());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.logContext = logContext;
    }

    protected BusinessException(ErrorCode errorCode, String detail, String logContext) {
        super(detail);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.logContext = logContext;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public java.util.Optional<String> logContext() {
        return java.util.Optional.ofNullable(logContext);
    }
}
