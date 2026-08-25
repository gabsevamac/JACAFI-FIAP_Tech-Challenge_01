package com.jacafi.tech.vehicle.domain;

/**
 * A vehicle was registered with a plate that already belongs to another active vehicle.
 *
 * <p>This is a business rule violation, not a database error: the check happens in the
 * application layer, before persisting, so that the failure carries domain meaning rather than
 * surfacing as a constraint violation. The unique index in the database is the second line of
 * defence, for the concurrent case.
 *
 * <p>Carries no plate, by design — not even the one that caused the conflict.
 */
public class DuplicateLicensePlateException extends RuntimeException {

    public DuplicateLicensePlateException(String message) {
        super(message);
    }
}
