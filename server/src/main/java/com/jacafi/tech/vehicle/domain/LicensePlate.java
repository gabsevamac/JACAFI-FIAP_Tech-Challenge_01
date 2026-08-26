package com.jacafi.tech.vehicle.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * A vehicle's official identification code: immutable, validated on construction, normalized to
 * upper case without separators, and equal by value.
 *
 * <p>It is also the vehicle's business identity — the workshop recognizes a returning vehicle by
 * its plate, not by a surrogate key.
 *
 * <p>This type owns both halves of the plate format: the expression that accepts it, and the
 * masking that renders it safe to log. Keeping them together is the point — they describe the
 * same seven characters, and a change to one that ignores the other would be a bug.
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

    private static final String MASK = "***";

    /**
     * Below this length, masking three characters would reveal more than it hides, so the value is
     * replaced entirely. A well-formed plate has seven characters and never reaches this case.
     */
    private static final int SHORTEST_MASKABLE_LENGTH = 5;

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
     * <p>Format: the first three characters, three asterisks, and the last one — {@code ABC***3}
     * for {@code ABC1D23}. Enough to correlate two log entries about the same vehicle during an
     * investigation, not enough to identify the vehicle or, through it, a person. A plate is
     * personal data (LGPD Art. 5 I), and Art. 6 VII keeps it out of logs, error messages and
     * stack traces.
     *
     * @param value any value that might be a plate; may be null
     * @return the masked rendering, never null and never the full value
     */
    public static String mask(String value) {
        if (value == null || value.length() < SHORTEST_MASKABLE_LENGTH) {
            return MASK;
        }
        return value.substring(0, 3) + MASK + value.charAt(value.length() - 1);
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
