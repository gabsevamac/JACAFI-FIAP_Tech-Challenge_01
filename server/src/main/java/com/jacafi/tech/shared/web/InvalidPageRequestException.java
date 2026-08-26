package com.jacafi.tech.shared.web;

/**
 * A paging or sorting parameter the API will not accept.
 *
 * <p>Extends {@link IllegalArgumentException} so the existing per-slice advices already answer 400
 * for it without a new handler.
 *
 * <p><strong>The message is generic on purpose, and must stay that way.</strong> Both advices copy
 * it into the response body, so anything specific said here is said to the client. Naming the
 * rejected sort field would confirm which attribute names exist on the entity, which is how an
 * unauthenticated caller maps a schema one guess at a time.
 */
public class InvalidPageRequestException extends IllegalArgumentException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
