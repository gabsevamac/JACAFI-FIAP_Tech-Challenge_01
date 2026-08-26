package com.jacafi.tech.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.jacafi.tech.customer.exception.InvalidTaxIdException;

class TaxIdTest {

    @Nested
    @DisplayName("the value decides which registration it is")
    class Dispatch {

        @ParameterizedTest
        @DisplayName("eleven characters is a CPF, whatever punctuation came with it")
        @ValueSource(strings = {"529.982.247-25", "52998224725", "  529982247-25  "})
        void readsACpf(String raw) {
            TaxId taxId = TaxId.of(raw);

            assertThat(taxId).isInstanceOf(Cpf.class);
            assertThat(taxId.value()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("fourteen characters is a CNPJ")
        void readsANumericCnpj() {
            TaxId taxId = TaxId.of("11.222.333/0001-81");

            assertThat(taxId).isInstanceOf(Cnpj.class);
            assertThat(taxId.value()).isEqualTo("11222333000181");
        }

        @Test
        @DisplayName("the alphanumeric CNPJ is upper-cased, not rejected")
        void readsAnAlphanumericCnpj() {
            TaxId taxId = TaxId.of("00.000.000/e08g-12");

            assertThat(taxId).isInstanceOf(Cnpj.class);
            assertThat(taxId.value()).isEqualTo("00000000E08G12");
        }

        @ParameterizedTest
        @DisplayName("a length that is neither is not a registration at all")
        @ValueSource(strings = {"", "123", "5299822472", "529982247251", "5299822472512"})
        void rejectsAnythingElse(String raw) {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of(raw));
        }

        @Test
        void rejectsNull() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of(null));
        }
    }

    @Nested
    class CpfRules {

        @Test
        void rejectsWrongCheckDigits() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("52998224724"));
        }

        @Test
        @DisplayName("a repeated digit passes the arithmetic but is never issued")
        void rejectsRepeatedDigits() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("11111111111"));
        }

        @Test
        @DisplayName("eleven characters that are not all digits is not a CPF")
        void rejectsLetters() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("5299822472A"));
        }
    }

    @Nested
    class CnpjRules {

        @Test
        void rejectsWrongCheckDigits() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("00.000.000/E08G-13"));
        }

        @Test
        void rejectsRepeatedCharacters() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("00000000000000"));
        }

        @Test
        @DisplayName("the two check digits must be numeric, even when the root is not")
        void rejectsLettersInTheCheckDigits() {
            assertThatExceptionOfType(InvalidTaxIdException.class).isThrownBy(() -> TaxId.of("00000000E08G1A"));
        }
    }

    @Nested
    @DisplayName("personal data protection")
    class Protection {

        @Test
        @DisplayName("the rejection message never echoes what was rejected")
        void doesNotLeakTheRejectedValue() {
            assertThatExceptionOfType(InvalidTaxIdException.class)
                    .isThrownBy(() -> TaxId.of("52998224724"))
                    .withMessageNotContaining("52998224724");
        }

        @Test
        void masksACpf() {
            assertThat(TaxId.of("52998224725").masked()).isEqualTo("529***25");
        }

        @Test
        void masksACnpj() {
            assertThat(TaxId.of("11222333000181").masked()).isEqualTo("112***81");
        }

        @Test
        @DisplayName("toString cannot leak the registration, on either type")
        void toStringIsMasked() {
            assertThat(TaxId.of("52998224725").toString())
                    .doesNotContain("52998224725")
                    .contains("529***25");
            assertThat(TaxId.of("11222333000181").toString())
                    .doesNotContain("11222333000181")
                    .contains("112***81");
        }
    }

    @Nested
    @DisplayName("the vendored validators expect input already normalized")
    class VendoredContract {

        @Test
        @DisplayName("CpfValidator rejects punctuation, and TaxId is what strips it")
        void cpfValidatorNeedsUnformattedInput() {
            assertThat(CpfValidator.isValid("529.982.247-25")).isFalse();
            assertThat(CpfValidator.isValid("52998224725")).isTrue();

            // Which is why the same value goes through the entry point without trouble.
            assertThat(TaxId.of("529.982.247-25").value()).isEqualTo("52998224725");
        }

        @Test
        @DisplayName("CnpjValidator does not upper-case, and TaxId is what does")
        void cnpjValidatorNeedsUpperCaseInput() {
            assertThat(CnpjValidator.isValid("00000000e08g12")).isFalse();
            assertThat(CnpjValidator.isValid("00000000E08G12")).isTrue();

            assertThat(TaxId.of("00.000.000/e08g-12").value()).isEqualTo("00000000E08G12");
        }
    }

    @Nested
    class Identity {

        @Test
        @DisplayName("equality is by value, after normalization")
        void equalsByNormalizedValue() {
            assertThat(TaxId.of("529.982.247-25")).isEqualTo(TaxId.of("52998224725"));
        }
    }
}
