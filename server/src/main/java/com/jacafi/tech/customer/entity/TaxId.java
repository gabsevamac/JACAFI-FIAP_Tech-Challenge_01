package com.jacafi.tech.customer.entity;

import java.util.Locale;

import com.jacafi.tech.customer.exception.InvalidTaxIdException;

/**
 * Fiscal identification of a customer: a {@link Cpf} for a natural person, a {@link Cnpj} for a
 * legal entity. §9 of the dictionary fixes all three names, and marks CPF and CNPJ as terms that
 * are not to be translated.
 *
 * <p>Sealed because the set is closed by law, not by convenience: Brazilian tax law defines these
 * two registrations and no third. Closing the hierarchy is what makes a {@code switch} over it
 * exhaustive, so adding a case later becomes a compile error at every point that decides on the
 * type rather than a silent fallthrough.
 *
 * <p>The type carries what an enum used to say. A pair of {@code (PersonType, String)} could hold
 * {@code INDIVIDUAL} next to a CNPJ — prevented by validation, but representable, and therefore
 * something tests had to cover. Here it cannot be written down.
 *
 * <p>Both implementations are personal data (LGPD Art. 5 I), which is why {@link #masked()} exists
 * and why neither {@code toString} prints the full value.
 */
public sealed interface TaxId permits Cpf, Cnpj {

    /** The normalized registration: digits only for a CPF, digits and letters for a CNPJ. */
    String value();

    /** The only rendering that may reach a log. */
    String masked();

    /**
     * Reads a registration from raw input, deciding what it is by its own shape.
     *
     * <p>The caller does not say which type it is, and cannot get that wrong: a normalized CPF has
     * eleven characters and a CNPJ fourteen, ranges that do not overlap. Asking a client to declare
     * the type as well only creates a way for the two answers to disagree.
     *
     * @throws InvalidTaxIdException when the input matches neither shape, or fails its check digits
     */
    static TaxId of(String raw) {
        if (raw == null) {
            throw new InvalidTaxIdException();
        }

        String normalized = normalize(raw);
        return switch (normalized.length()) {
            case 11 -> new Cpf(normalized);
            case 14 -> new Cnpj(normalized);
            default -> throw new InvalidTaxIdException();
        };
    }

    /**
     * Strips the punctuation a human types for readability and upper-cases the result, since the
     * alphanumeric CNPJ uses capital letters.
     */
    static String normalize(String raw) {
        return raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(".", "")
                .replace("/", "")
                .replace("-", "");
    }
}
