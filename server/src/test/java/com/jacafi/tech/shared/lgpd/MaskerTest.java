package com.jacafi.tech.shared.lgpd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("masking personal data for logs")
class MaskerTest {

    @Nested
    @DisplayName("a license plate")
    class LicensePlates {

        @Test
        @DisplayName("keeps the three leading characters of a Mercosur plate")
        void masksAMercosurPlate() {
            assertThat(Masker.licensePlate("ABC1D23")).isEqualTo("ABC****");
        }

        @Test
        @DisplayName("keeps the three leading characters of a pre-Mercosur plate")
        void masksAPreMercosurPlate() {
            assertThat(Masker.licensePlate("ABC1234")).isEqualTo("ABC****");
        }

        @Test
        @DisplayName("masks both layouts identically, so the survivors do not classify the plate")
        void doesNotRevealWhichLayoutItWas() {
            assertThat(Masker.licensePlate("ABC1D23")).isEqualTo(Masker.licensePlate("ABC1234"));
        }

        @Test
        @DisplayName("never contains the full value")
        void neverEchoesTheValue() {
            assertThat(Masker.licensePlate("ABC1D23")).doesNotContain("1D23");
        }
    }

    @Nested
    @DisplayName("a taxpayer registration")
    class Documents {

        @Test
        @DisplayName("keeps the three trailing digits of a CPF")
        void masksACpf() {
            assertThat(Masker.document("52998224725")).isEqualTo("********725");
        }

        @Test
        @DisplayName("keeps the three trailing characters of a CNPJ")
        void masksACnpj() {
            assertThat(Masker.document("11222333000181")).isEqualTo("***********181");
        }

        @Test
        @DisplayName("hides the leading digits, which correlate with the issuing region")
        void hidesTheRegionalPrefix() {
            assertThat(Masker.document("52998224725")).doesNotContain("529");
        }

        @Test
        @DisplayName("keeps the length, which is what already distinguishes a CPF from a CNPJ")
        void preservesLength() {
            assertThat(Masker.document("52998224725")).hasSize(11);
            assertThat(Masker.document("11222333000181")).hasSize(14);
        }
    }

    @Nested
    @DisplayName("an e-mail address")
    class Emails {

        @Test
        @DisplayName("keeps the first character and the whole domain")
        void masksTheLocalPart() {
            assertThat(Masker.email("mariana@example.com")).isEqualTo("m***@example.com");
        }

        @Test
        @DisplayName("hides the length of the local part")
        void doesNotRevealLocalPartLength() {
            // Same initial, different lengths: the initial is kept by design, so it has to be held
            // constant for this to measure what it claims to measure.
            assertThat(Masker.email("mariana@example.com")).isEqualTo(Masker.email("marcos@example.com"));
        }

        @ParameterizedTest
        @DisplayName("masks entirely what is not a usable address")
        @ValueSource(strings = {"nao-e-um-email", "@example.com", "mariana@"})
        void masksMalformedInputEntirely(String value) {
            assertThat(Masker.email(value)).isEqualTo("***");
        }
    }

    @Nested
    @DisplayName("degenerate input")
    class Degenerate {

        @ParameterizedTest
        @DisplayName("null and empty are masked rather than thrown on")
        @NullAndEmptySource
        void survivesNullAndEmpty(String value) {
            assertThat(Masker.licensePlate(value)).isEqualTo("***");
            assertThat(Masker.document(value)).isEqualTo("***");
            assertThat(Masker.email(value)).isEqualTo("***");
        }

        @ParameterizedTest
        @DisplayName("a value too short to hide at least half of is masked entirely")
        @ValueSource(strings = {"A", "AB", "ABC", "ABCD", "ABCDE", " "})
        void masksShortValuesEntirely(String value) {
            assertThat(Masker.licensePlate(value)).isEqualTo("***");
            assertThat(Masker.document(value)).isEqualTo("***");
        }

        @Test
        @DisplayName("a value longer than any of these formats does not get a run of asterisks per character")
        void boundsTheAsteriskRun() {
            // The token that replaces a plate on removal. Proportional masking rendered it as
            // three characters and thirty-eight asterisks: unreadable, and disclosing a length
            // that tells the reader nothing they can use.
            assertThat(Masker.licensePlate("ANON-0f7c9a1e-3b2d-4c5f-8a9b-1d2e3f4a5b6c"))
                    .isEqualTo("ANO***********");
        }
    }
}
