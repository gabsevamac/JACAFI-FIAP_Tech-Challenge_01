package com.jacafi.tech.shared.web;

import java.util.Objects;

/**
 * A failure the client caused and can act on, carrying the code that says which.
 *
 * <p>Lives in {@code shared} so the slices depend on it rather than the other way round: a global
 * advice that imported every slice's exceptions would make the shared package depend on all four,
 * and adding a fifth slice would mean editing shared code.
 *
 * <p>The distinction this type draws is the one the error handling rests on. A
 * {@code BusinessException} means something the caller can understand and fix, so its message is
 * safe to return. Every other exception — including the {@code IllegalArgumentException} that a
 * domain invariant throws with a message like {@code "vehicleId must not be null"} — is a
 * programmer error, and its message is written for whoever reads the log, not for the client.
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

    /**
     * For the rare case where the catalogue's text needs narrowing.
     *
     * <p>{@code detail} goes to the client, so it is under the same rule as the catalogue: no
     * submitted value, no plate, no registration, no identifier of a record the caller may not
     * know exists.
     */
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
