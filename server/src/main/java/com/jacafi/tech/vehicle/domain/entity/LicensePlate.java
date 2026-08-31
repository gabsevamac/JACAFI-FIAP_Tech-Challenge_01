package com.jacafi.tech.vehicle.domain.entity;

import java.util.Locale;
import java.util.regex.Pattern;

import com.jacafi.tech.vehicle.domain.exception.InvalidLicensePlateException;

public record LicensePlate(String value) {

    private static final Pattern SEPARATORS = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    public LicensePlate {
        if (value == null) {
            throw new InvalidLicensePlateException();
        }
        value = SEPARATORS.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidLicensePlateException();
        }
    }

    public String masked() {
        return mask(value);
    }

    public static String mask(String value) {
        if (value == null || value.length() < 4) {
            return "***";
        }
        return value.substring(0, 3) + "*".repeat(value.length() - 3);
    }

    @Override
    public String toString() {
        return "LicensePlate[" + masked() + "]";
    }
}
