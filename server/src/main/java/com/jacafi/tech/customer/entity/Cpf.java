package com.jacafi.tech.customer.entity;

import com.jacafi.tech.customer.exception.InvalidTaxIdException;
import com.jacafi.tech.shared.lgpd.PersonalData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registration of a natural person with the federal revenue service. Not translated, per §9: it is
 * a term of the Brazilian legal order and has no English equivalent.
 *
 * <p>Eleven digits, the last two being check digits, which {@link CpfValidator} verifies. That
 * class sits in this same file on purpose: it is vendored code whose provenance travels with it,
 * and keeping the two together means nobody has to go looking for where the arithmetic came from.
 *
 * @param value the normalized registration, digits only
 */
public record Cpf(@PersonalData("LGPD Art. 5 I — identifies a natural person directly")
                  String value) implements TaxId {

    public Cpf {
        if (value == null) {
            throw new InvalidTaxIdException();
        }
        value = TaxId.normalize(value);
        if (!CpfValidator.isValid(value)) {
            // The rejected value never appears in the message: it is personal data even when
            // invalid (LGPD Art. 6 VII).
            throw new InvalidTaxIdException();
        }
    }

    /** First three and last two characters — enough to correlate log lines, not to identify. */
    @Override
    public String masked() {
        return value.substring(0, 3) + "***" + value.substring(9);
    }

    /**
     * Masked on purpose. A record's generated {@code toString} would print the whole registration,
     * and interpolating a customer into a log statement is how that happens by accident.
     */
    @Override
    public String toString() {
        return "Cpf[" + masked() + "]";
    }
}

/**
 * CPF (Cadastro de Pessoas Físicas) check-digit validation.
 *
 * <p>Extracted from Caelum Stella's {@code CPFValidator}, version 2.2.2, with all framework
 * dependencies removed. The validation logic itself is unchanged: same order of checks, same digit
 * calculation, same acceptance criteria.
 *
 * <p>Copyright Caelum. Author: Leonardo Bessa. Licensed under the Apache License, Version 2.0.
 * Source: https://github.com/caelum/caelum-stella/blob/master/stella-core/src/main/java/br/com/caelum/stella/validation/CPFValidator.java
 *
 * <p>This file has been modified from the original. Section 4(b) of the licence requires those
 * changes to be stated, and they are listed in {@code THIRD-PARTY-NOTICES.md} at the root of the
 * repository, together with the licence text required by section 4(a).
 *
 * <p>Behaviour mirrors Stella's default constructor {@code new CPFValidator()}, which means
 * {@code isFormatted = false} and {@code isIgnoringRepeatedDigits = false}. See the note on
 * {@link #isValid(String)} — the first of those two defaults has a consequence that is easy to
 * miss.
 *
 * <p>A valid number here is one that is mathematically well formed. It says nothing about whether
 * the CPF was actually issued or is currently active — only the Receita Federal can answer that.
 *
 * <p>Package-private, and deliberately so: {@link TaxId#of} is the only supported way into this
 * validation, and it normalizes before calling.
 */
final class CpfValidator {

    private static final Pattern FORMATTED = Pattern.compile("(\\d{3})[.](\\d{3})[.](\\d{3})-(\\d{2})");
    private static final Pattern UNFORMATTED = Pattern.compile("(\\d{3})(\\d{3})(\\d{3})(\\d{2})");

    private CpfValidator() {
    }

    /**
     * Returns whether the given string is a valid CPF.
     *
     * <p><strong>Accepts unformatted input only</strong> — {@code "12345678909"} passes,
     * {@code "123.456.789-09"} does not. This reproduces Stella's default, where the
     * {@code isFormatted} flag defaults to {@code false} and the validator rejects any input whose
     * formatting does not match that expectation. To accept both forms, remove the
     * {@code REJECT_FORMATTED_INPUT} block below.
     *
     * <p>In this codebase that quirk is inert: {@link Cpf} strips punctuation through
     * {@link TaxId#normalize} before calling, so what arrives here is always unformatted. The
     * block is kept for fidelity to the original, and because deleting it would quietly change
     * what this class means if it is ever reused elsewhere.
     *
     * @param cpf the candidate number; {@code null} returns {@code false}
     */
    static boolean isValid(String cpf) {
        if (cpf == null) {
            return false;
        }

        // --- REJECT_FORMATTED_INPUT ---
        // Stella: `if (isFormatted != FORMATED.matcher(cpf).matches())` with isFormatted = false.
        // Delete these three lines to accept "123.456.789-09" as well.
        if (FORMATTED.matcher(cpf).matches()) {
            return false;
        }
        // --- end ---

        String unformatted = unformat(cpf);
        if (unformatted == null) {
            return false;
        }

        // Stella keeps this check after unformat. It is unreachable in practice, since unformat
        // already guarantees exactly 11 digits — preserved here for fidelity to the original.
        if (unformatted.length() != 11 || !unformatted.matches("[0-9]*")) {
            return false;
        }

        if (hasAllRepeatedDigits(unformatted)) {
            return false;
        }

        String base = unformatted.substring(0, unformatted.length() - 2);
        String checkDigits = unformatted.substring(unformatted.length() - 2);

        return checkDigits.equals(calculateCheckDigits(base));
    }

    /**
     * Strips punctuation, accepting either the formatted or the unformatted form.
     *
     * <p>Replaces Stella's {@code CPFFormatter.unformat}, which threw
     * {@code IllegalArgumentException} on unrecognised input; here that case returns {@code null}.
     */
    private static String unformat(String cpf) {
        Matcher matcher = FORMATTED.matcher(cpf);
        if (matcher.matches()) {
            return matcher.replaceAll("$1$2$3$4");
        }
        matcher = UNFORMATTED.matcher(cpf);
        if (matcher.matches()) {
            return matcher.replaceAll("$1$2$3$4");
        }
        return null;
    }

    /**
     * Calculates both check digits from the 9-digit base.
     *
     * <p>Stella expresses this through its {@code DigitoPara} builder:
     * {@code comMultiplicadoresDeAte(2, 11).complementarAoModulo().trocandoPorSeEncontrar("0", 10, 11).mod(11)}.
     * Spelled out, that is: multiply right to left by weights starting at 2, sum, take modulo 11,
     * subtract the remainder from 11, and map results of 10 or 11 to zero. The second digit is
     * calculated the same way over the base plus the first digit.
     */
    private static String calculateCheckDigits(String base) {
        String first = calculateCheckDigit(base);
        String second = calculateCheckDigit(base + first);
        return first + second;
    }

    private static String calculateCheckDigit(String base) {
        int sum = 0;
        int weight = 2;

        for (int i = base.length() - 1; i >= 0; i--) {
            sum += Character.digit(base.charAt(i), 10) * weight;
            weight++;
            // Stella's weight range is 2..11 and wraps around. For CPF the wrap never triggers
            // (9 or 10 digits at most), but it is kept so the behaviour matches the original.
            if (weight > 11) {
                weight = 2;
            }
        }

        int result = 11 - (sum % 11);
        return (result == 10 || result == 11) ? "0" : String.valueOf(result);
    }

    /**
     * Sequences such as "11111111111" satisfy the check-digit maths but are not valid CPFs, so they
     * are rejected explicitly.
     */
    private static boolean hasAllRepeatedDigits(String cpf) {
        for (int i = 1; i < cpf.length(); i++) {
            if (cpf.charAt(i) != cpf.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
