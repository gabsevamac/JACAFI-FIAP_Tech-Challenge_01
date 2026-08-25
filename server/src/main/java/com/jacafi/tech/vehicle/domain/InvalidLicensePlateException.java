package com.jacafi.tech.vehicle.domain;

/**
 * A license plate was rejected for not matching either accepted Brazilian format.
 *
 * <p>Extends {@link IllegalArgumentException} because that is what it is: the caller supplied an
 * argument the domain cannot accept. The message never carries the offending value.
 */
public class InvalidLicensePlateException extends IllegalArgumentException {

    public InvalidLicensePlateException(String message) {
        super(message);
    }
}
