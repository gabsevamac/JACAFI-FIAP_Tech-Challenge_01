package com.jacafi.tech.client.entity;

import com.jacafi.tech.client.exception.InvalidTaxIdentifierException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Locale;
import java.util.Objects;

@Embeddable
public class TaxIdentifier {

    private static final int[] CPF_FIRST_DIGIT_WEIGHTS = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CPF_SECOND_DIGIT_WEIGHTS = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, updatable = false, length = 20)
    private PersonType personType;

    @Column(name = "tax_id", nullable = false, updatable = false, length = 14)
    private String value;

    protected TaxIdentifier() {
    }

    private TaxIdentifier(PersonType personType, String value) {
        this.personType = personType;
        this.value = value;
    }

    public static TaxIdentifier of(PersonType personType, String rawValue) {
        if (personType == null || rawValue == null) {
            throw new InvalidTaxIdentifierException();
        }

        var normalizedValue = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace(".", "")
                .replace("/", "")
                .replace("-", "");

        var valid = switch (personType) {
            case INDIVIDUAL -> isValidCpf(normalizedValue);
            case LEGAL_ENTITY -> isValidCnpj(normalizedValue);
        };

        if (!valid) {
            throw new InvalidTaxIdentifierException();
        }

        return new TaxIdentifier(personType, normalizedValue);
    }

    public PersonType getPersonType() {
        return personType;
    }

    public String getValue() {
        return value;
    }

    private static boolean isValidCpf(String value) {
        if (!value.matches("[0-9]{11}") || value.chars().distinct().count() == 1) {
            return false;
        }

        return calculateDigit(value.substring(0, 9), CPF_FIRST_DIGIT_WEIGHTS, false) == value.charAt(9) - '0'
                && calculateDigit(value.substring(0, 10), CPF_SECOND_DIGIT_WEIGHTS, false) == value.charAt(10) - '0';
    }

    private static boolean isValidCnpj(String value) {
        if (!value.matches("[0-9A-Z]{12}[0-9]{2}") || value.chars().distinct().count() == 1) {
            return false;
        }

        return calculateDigit(value.substring(0, 12), CNPJ_FIRST_DIGIT_WEIGHTS, true) == value.charAt(12) - '0'
                && calculateDigit(value.substring(0, 13), CNPJ_SECOND_DIGIT_WEIGHTS, true) == value.charAt(13) - '0';
    }

    private static int calculateDigit(String value, int[] weights, boolean alphanumeric) {
        var sum = 0;
        for (var index = 0; index < weights.length; index++) {
            var characterValue = alphanumeric ? value.charAt(index) - 48 : value.charAt(index) - '0';
            sum += characterValue * weights[index];
        }
        var remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TaxIdentifier that)) {
            return false;
        }
        return personType == that.personType && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personType, value);
    }
}
