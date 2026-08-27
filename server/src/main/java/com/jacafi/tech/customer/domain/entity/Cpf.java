package com.jacafi.tech.customer.domain.entity;

import com.jacafi.tech.customer.domain.exception.InvalidTaxIdException;

public record Cpf(String value) implements TaxId {

    public Cpf {
        if (value == null
                || !TaxId.CPF.matcher(value).matches()
                || hasRepeatedDigits(value)
                || !hasValidCheckDigits(value)) {
            throw new InvalidTaxIdException();
        }
    }

    @Override
    public String masked() {
        return TaxId.mask(value);
    }

    @Override
    public String toString() {
        return "Cpf[" + masked() + "]";
    }

    private static boolean hasValidCheckDigits(String value) {
        return checkDigit(value.substring(0, 9), 10) == Character.digit(value.charAt(9), 10)
                && checkDigit(value.substring(0, 10), 11) == Character.digit(value.charAt(10), 10);
    }

    private static int checkDigit(String base, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < base.length(); index++) {
            sum += Character.digit(base.charAt(index), 10) * (initialWeight - index);
        }
        int result = (sum * 10) % 11;
        return result == 10 ? 0 : result;
    }

    private static boolean hasRepeatedDigits(String value) {
        return value.chars().distinct().count() == 1;
    }
}
