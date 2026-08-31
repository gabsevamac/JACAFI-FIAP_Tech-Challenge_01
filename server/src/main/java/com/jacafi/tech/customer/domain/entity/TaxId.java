package com.jacafi.tech.customer.domain.entity;

import java.util.Locale;
import java.util.regex.Pattern;

import com.jacafi.tech.customer.domain.exception.InvalidTaxIdException;

public sealed interface TaxId permits Cpf, Cnpj {

    Pattern CPF = Pattern.compile("\\d{11}");
    Pattern FORMATTED_CPF = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    Pattern CNPJ = Pattern.compile("[A-Z0-9]{12}\\d{2}");
    Pattern FORMATTED_CNPJ = Pattern.compile("[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/[A-Z0-9]{4}-\\d{2}");

    String value();

    String masked();

    static TaxId of(String rawValue) {
        String normalized = normalize(rawValue);
        return normalized.length() == 11 ? new Cpf(normalized) : new Cnpj(normalized);
    }

    static String mask(String value) {
        int visibleCharacters = Math.min(3, value.length());
        return "*".repeat(value.length() - visibleCharacters) + value.substring(value.length() - visibleCharacters);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new InvalidTaxIdException();
        }

        String value = rawValue.trim().toUpperCase(Locale.ROOT);
        if (CPF.matcher(value).matches() || CNPJ.matcher(value).matches()) {
            return value;
        }
        if (FORMATTED_CPF.matcher(value).matches()
                || FORMATTED_CNPJ.matcher(value).matches()) {
            return value.replace(".", "").replace("/", "").replace("-", "");
        }
        throw new InvalidTaxIdException();
    }
}
