package com.jacafi.tech.vehicle.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

    @Test
    @DisplayName("accepts the pre-Mercosur format")
    void acceptsTheOldBrazilianFormat() {
        assertThat(new LicensePlate("ABC1234").value()).isEqualTo("ABC1234");
    }

    @Test
    @DisplayName("accepts the Mercosur format, where the fifth character is a letter")
    void acceptsTheMercosurFormat() {
        assertThat(new LicensePlate("ABC1D23").value()).isEqualTo("ABC1D23");
    }

    @ParameterizedTest
    @DisplayName("normalizes lower case, separators and surrounding whitespace")
    @ValueSource(strings = {"abc1234", "abc-1234", "ABC-1234", "  abc 1234  ", "a.b.c.1.2.3.4"})
    void normalizesBeforeValidating(String input) {
        assertThat(new LicensePlate(input).value()).isEqualTo("ABC1234");
    }

    @ParameterizedTest
    @DisplayName("rejects anything that is not one of the two accepted layouts")
    @ValueSource(
            strings = {
                "ABC123", // one character short
                "ABC12345", // one character too long
                "AB12345", // only two letters in the prefix
                "ABCD123", // fourth character must be a digit
                "ABC12E3", // last two characters must be digits
                "1234ABC", // digits and letters swapped
                "", // empty
                "   " // whitespace only
            })
    void rejectsInvalidFormats(String input) {
        assertThatExceptionOfType(InvalidLicensePlateException.class).isThrownBy(() -> new LicensePlate(input));
    }

    @Test
    void rejectsNull() {
        assertThatExceptionOfType(InvalidLicensePlateException.class).isThrownBy(() -> new LicensePlate(null));
    }

    @Test
    @DisplayName("the rejection message never echoes the value that was rejected")
    void doesNotLeakTheRejectedValue() {
        assertThatExceptionOfType(InvalidLicensePlateException.class)
                .isThrownBy(() -> new LicensePlate("XYZ98765"))
                .withMessageNotContaining("XYZ98765");
    }

    @Test
    @DisplayName("equality is by value, after normalization")
    void equalsByNormalizedValue() {
        LicensePlate typedByHand = new LicensePlate("abc-1234");
        LicensePlate fromStorage = new LicensePlate("ABC1234");

        assertThat(typedByHand).isEqualTo(fromStorage).hasSameHashCodeAs(fromStorage);
    }

    @Test
    void differentPlatesAreNotEqual() {
        assertThat(new LicensePlate("ABC1234")).isNotEqualTo(new LicensePlate("ABC1D23"));
    }

    @Test
    @DisplayName("toString cannot leak the full plate")
    void toStringIsMasked() {
        LicensePlate plate = new LicensePlate("ABC1234");

        assertThat(plate.toString()).doesNotContain("ABC1234").contains("ABC***4");
    }

    @Nested
    @DisplayName("masking for logs")
    class Masking {

        @ParameterizedTest
        @DisplayName("keeps the first three characters and the last one")
        @CsvSource({"ABC1234, ABC***4", "ABC1D23, ABC***3", "XYZ9876, XYZ***6"})
        void masksTheMiddleOfAPlate(String plate, String expected) {
            assertThat(new LicensePlate(plate).masked()).isEqualTo(expected);
            assertThat(LicensePlate.mask(plate)).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("masks short values entirely, where partial masking would reveal too much")
        @ValueSource(strings = {"", "A", "AB", "ABC", "ABCD"})
        void masksShortValuesEntirely(String value) {
            assertThat(LicensePlate.mask(value)).isEqualTo("***");
        }

        @Test
        void masksNull() {
            assertThat(LicensePlate.mask(null)).isEqualTo("***");
        }

        @Test
        @DisplayName("masks an anonymization token, which can never be a valid plate")
        void masksTheAnonymizationToken() {
            assertThat(LicensePlate.mask("ANON-0f7c9a1e-3b2d-4c5f-8a9b-1d2e3f4a5b6c"))
                    .isEqualTo("ANO***c");
        }

        @Test
        @DisplayName("masks input the format check has already rejected")
        void masksRejectedInput() {
            assertThat(LicensePlate.mask("XYZ98765")).isEqualTo("XYZ***5");
        }
    }
}
