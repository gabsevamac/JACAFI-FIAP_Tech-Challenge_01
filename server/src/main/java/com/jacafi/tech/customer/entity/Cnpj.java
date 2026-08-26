package com.jacafi.tech.customer.entity;

import com.jacafi.tech.customer.exception.InvalidTaxIdException;
import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * Registration of a legal entity with the federal revenue service. Not translated, per §9.
 *
 * <p>Fourteen characters: twelve of root and branch, which under the alphanumeric rule may be
 * letters as well as digits, followed by two numeric check digits. {@link CnpjValidator}, in this
 * same file, verifies them.
 *
 * @param value the normalized registration, upper case
 */
public record Cnpj(@PersonalData("LGPD Art. 5 I — identifies a legal entity and, in a single-member "
                          + "company, a natural person through it")
                   String value) implements TaxId {

    public Cnpj {
        if (value == null) {
            throw new InvalidTaxIdException();
        }
        value = TaxId.normalize(value);
        if (!CnpjValidator.isValid(value)) {
            throw new InvalidTaxIdException();
        }
    }

    @Override
    public String masked() {
        return value.substring(0, 3) + "***" + value.substring(12);
    }

    @Override
    public String toString() {
        return "Cnpj[" + masked() + "]";
    }
}

/**
 * CNPJ check-digit validation, covering the alphanumeric registration.
 *
 * <p>Reference implementation published by the Receita Federal do Brasil, among the technical
 * documents for the CNPJ:
 * https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj/codigos-cnpj.zip/view
 *
 * <p>Vendored with structure and identifiers untouched — the Portuguese names ({@code calculaDV},
 * {@code PESOS_DV}) are part of its provenance, and renaming them would make it harder to diff
 * against the source it came from. The terms of use of that package have not been verified; see
 * {@code THIRD-PARTY-NOTICES.md}.
 *
 * <p>Two things are worth knowing before reusing it elsewhere. It tolerates {@code . / -} but does
 * <em>not</em> upper-case, so a lower-case alphanumeric CNPJ is rejected; and it rejects only the
 * all-zero sequence, not repeated characters in general — those fail the check digits anyway, which
 * is why the special case exists for zeros alone. Neither matters here: {@link Cnpj} normalizes
 * through {@link TaxId#normalize} first.
 *
 * <p>Deviation from the original, and the only one: the {@code IllegalArgumentException} thrown by
 * {@link #calculaDV(String)} no longer interpolates the registration into its message. A CNPJ is
 * personal data (LGPD Art. 5 I) and an exception message is a log line waiting to happen
 * (Art. 6 VII). The arithmetic is untouched.
 *
 * <p>Validity here means mathematically well formed. Whether the registration was issued or is
 * active only the Receita Federal can say.
 */
final class CnpjValidator {

    private static final int TAMANHO_CNPJ_SEM_DV = 12;
    private static final String REGEX_CARACTERES_FORMATACAO = "[./-]";
    private static final String REGEX_FORMACAO_BASE_CNPJ = "[A-Z\\d]{12}";
    private static final String REGEX_FORMACAO_DV = "[\\d]{2}";
    private static final String REGEX_VALOR_ZERADO = "^[0]+$";

    private static final int VALOR_BASE = (int) '0';
    private static final int[] PESOS_DV = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    static boolean isValid(String cnpj) {
        if (cnpj != null) {
            cnpj = removeCaracteresFormatacao(cnpj);
            if (isCnpjFormacaoValidaComDV(cnpj)) {
                String dvInformado = cnpj.substring(TAMANHO_CNPJ_SEM_DV);
                String dvCalculado = calculaDV(cnpj.substring(0, TAMANHO_CNPJ_SEM_DV));
                return dvCalculado.equals(dvInformado);
            }
        }

        return false;
    }

    static String calculaDV(String baseCnpj) {
        if (baseCnpj != null) {
            baseCnpj = removeCaracteresFormatacao(baseCnpj);

            if (isCnpjFormacaoValidaSemDV(baseCnpj)) {
                String dv1 = String.format("%d", calculaDigito(baseCnpj));
                String dv2 = String.format("%d", calculaDigito(baseCnpj.concat(dv1)));
                return dv1.concat(dv2);
            }
        }

        // The original interpolated the value here. See the class javadoc.
        throw new IllegalArgumentException("The given base is not a valid CNPJ for check digit calculation");
    }

    /**
     * Each character contributes its code point minus that of '0' — the digit's own value for
     * '0'..'9', and the value the alphanumeric rule assigns to 'A'..'Z'.
     */
    private static int calculaDigito(String cnpj) {
        int soma = 0;
        for (int indice = cnpj.length() - 1; indice >= 0; indice--) {
            int valorCaracter = (int) cnpj.charAt(indice) - VALOR_BASE;
            soma += valorCaracter * PESOS_DV[PESOS_DV.length - cnpj.length() + indice];
        }
        return soma % 11 < 2 ? 0 : 11 - (soma % 11);
    }

    private static String removeCaracteresFormatacao(String cnpj) {
        return cnpj.trim().replaceAll(REGEX_CARACTERES_FORMATACAO, "");
    }

    private static boolean isCnpjFormacaoValidaSemDV(String cnpj) {
        return cnpj.matches(REGEX_FORMACAO_BASE_CNPJ) && !cnpj.matches(REGEX_VALOR_ZERADO);
    }

    private static boolean isCnpjFormacaoValidaComDV(String cnpj) {
        return cnpj.matches(REGEX_FORMACAO_BASE_CNPJ.concat(REGEX_FORMACAO_DV))
                && !cnpj.matches(REGEX_VALOR_ZERADO);
    }
}
