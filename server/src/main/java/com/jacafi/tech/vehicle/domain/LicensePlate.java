package com.jacafi.tech.vehicle.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import com.jacafi.tech.shared.lgpd.Masker;
import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * A vehicle's official identification code: immutable, validated on construction, normalized to
 * upper case without separators, and equal by value.
 *
 * <p>It is also the vehicle's business identity — the workshop recognizes a returning vehicle by
 * its plate, not by a surrogate key.
 *
 * <p>This type owns the expression that accepts a plate. It no longer owns the masking: that
 * moved to {@link Masker} when the customer slice turned out to be masking registrations to a
 * different shape, which would have produced a log where two slices describe the same person two
 * ways.
 *
 * @param value the normalized plate, seven characters, no separator
 */
public record LicensePlate(
        @PersonalData("LGPD Art. 5 I — identifies a vehicle and, through it, its owner")
        String value) {

    /**
     * Both Brazilian formats in one expression: the pre-Mercosur {@code ABC1234} and the Mercosur
     * {@code ABC1D23}. The fifth character is the only position accepting either a letter or a
     * digit, which is precisely what distinguishes the two layouts.
     */
    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    /** Anything that is not a letter or a digit is a separator a human typed for readability. */
    private static final Pattern SEPARATORS = Pattern.compile("[^A-Za-z0-9]");

    public LicensePlate {
        if (value == null) {
            throw new InvalidLicensePlateException("A license plate is required.");
        }
        value = SEPARATORS.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            // The rejected value is deliberately absent from the message. Even an invalid plate
            // is data a person typed, and an error message is not a place for it (LGPD Art. 6 VII).
            throw new InvalidLicensePlateException(
                    "A license plate must match either the ABC1234 or the ABC1D23 format.");
        }
    }

    /** The only rendering of this plate that may be logged. */
    public String masked() {
        return mask(value);
    }

    /**
     * Masks a value that may be a plate but is not, or cannot be, a valid one — the irreversible
     * token that replaces a plate after removal, or input the format check has just rejected.
     * Those values cannot exist as an instance of this type, which is why this entry point takes
     * a raw string.
     *
     * <p>Delegates to {@link Masker}, which owns the format for every slice. The rendering is
     * {@code ABC****}: enough to correlate two log entries about the same vehicle during an
     * investigation, not enough to identify the vehicle or, through it, a person.
     *
     * @param value any value that might be a plate; may be null
     * @return the masked rendering, never null and never the full value
     */
    public static String mask(String value) {
        return Masker.licensePlate(value);
    }

    /**
     * Masked on purpose. A record's generated {@code toString} would print the full plate, and
     * string interpolation in a log statement is the easiest way for that to happen by accident.
     */
    @Override
    public String toString() {
        return "LicensePlate[" + masked() + "]";
    }
}
