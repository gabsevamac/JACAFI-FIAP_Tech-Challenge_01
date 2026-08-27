package com.jacafi.tech.shared.domain;

import java.util.Objects;

/**
 * A failure the client caused and can act on, carrying the code that says which.
 *
 * <p>Lives in the shared domain so business code can use it without depending on HTTP or Spring.
 *
 * <p>The distinction this type draws is the one the error handling rests on. A
 * {@code BusinessException} means something the caller can understand and fix, so its stable error
 * code is safe to return. Exception messages are diagnostic text and never form part of the HTTP
 * response. Every other exception — including the {@code IllegalArgumentException} that a domain
 * invariant throws with a message like {@code "vehicleId must not be null"} — is a programmer
 * error, and its message is written for whoever reads the log, not for the client.
 */
public abstract class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final String logContext;

    protected BusinessException(ErrorCode errorCode) {
        this(errorCode, (String) null);
    }

    /**
     * @param logContext what an operator needs to find the record, written to the log and never to
     *                   the response. A surrogate identifier belongs here; a plate or a
     *                   registration does not, masked or otherwise — the trail is the place for
     *                   values, and a log line is not.
     */
    protected BusinessException(ErrorCode errorCode, String logContext) {
        super(errorCode.message());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.logContext = logContext;
    }

    /** Retains a diagnostic exception message for compatibility; adapters must not return it. */
    protected BusinessException(ErrorCode errorCode, String detail, String logContext) {
        super(detail);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.logContext = logContext;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    /** Empty unless the thrower had something an operator would want in the log. */
    public java.util.Optional<String> logContext() {
        return java.util.Optional.ofNullable(logContext);
    }
}
