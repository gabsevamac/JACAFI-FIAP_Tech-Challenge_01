package com.jacafi.tech.customer.domain.entity;

import com.jacafi.tech.customer.domain.exception.InvalidTaxIdException;

public record Cnpj(String value) implements TaxId {

    private static final int[] FIRST_CHECK_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_CHECK_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        if (value == null || !TaxId.CNPJ.matcher(value).matches() || isAllZero(value) || !hasValidCheckDigits(value)) {
            throw new InvalidTaxIdException();
        }
    }

    @Override
    public String masked() {
        return TaxId.mask(value);
    }

    @Override
    public String toString() {
        return "Cnpj[" + masked() + "]";
    }

    private static boolean hasValidCheckDigits(String value) {
        String base = value.substring(0, 12);
        int firstCheckDigit = checkDigit(base, FIRST_CHECK_DIGIT_WEIGHTS);
        int secondCheckDigit = checkDigit(base + firstCheckDigit, SECOND_CHECK_DIGIT_WEIGHTS);
        return firstCheckDigit == Character.digit(value.charAt(12), 10)
                && secondCheckDigit == Character.digit(value.charAt(13), 10);
    }

    private static int checkDigit(String value, int[] weights) {
        int sum = 0;
        for (int index = 0; index < value.length(); index++) {
            sum += (value.charAt(index) - '0') * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean isAllZero(String value) {
        return value.chars().allMatch(character -> character == '0');
    }
}
