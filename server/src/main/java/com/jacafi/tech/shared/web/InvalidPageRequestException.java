package com.jacafi.tech.shared.web;

/**
 * A paging or sorting parameter the API will not accept.
 *
 * <p>A {@link BusinessException} because its message is one of the few written for the client: it
 * says what to change without saying anything about the resource. Everything else that reaches the
 * handler as a bad argument is answered generically, because those messages describe invariants.
 *
 * <p><strong>The messages must stay free of the submitted value.</strong> Naming the rejected sort
 * field would confirm which attribute names are being probed, which is how a caller maps the
 * schema one request at a time.
 */
public class InvalidPageRequestException extends BusinessException {

    public InvalidPageRequestException(String message) {
        super(ErrorCode.INVALID_PAGING, message, null);
    }
}
