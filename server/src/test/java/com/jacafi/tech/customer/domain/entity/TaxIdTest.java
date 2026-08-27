package com.jacafi.tech.customer.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.jacafi.tech.customer.domain.exception.InvalidTaxIdException;

class TaxIdTest {

    @Test
    void normalizesAndValidatesCpfUsingBothCheckDigits() {
        TaxId taxId = TaxId.of(" 529.982.247-25 ");

        assertThat(taxId).isInstanceOf(Cpf.class);
        assertThat(taxId.value()).isEqualTo("52998224725");
        assertThat(taxId.masked()).isEqualTo("********725");
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "529.982247-25", "5299822472A"})
    void rejectsInvalidOrMalformedCpf(String rawTaxId) {
        assertThatThrownBy(() -> TaxId.of(rawTaxId)).isInstanceOf(InvalidTaxIdException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.ABC.345/01DE-35", "00.000.000/E08G-12", "12.abc.345/01de-35"})
    void acceptsOfficialAlphanumericCnpjValues(String rawTaxId) {
        TaxId taxId = TaxId.of(rawTaxId);

        assertThat(taxId).isInstanceOf(Cnpj.class);
        assertThat(taxId.value()).hasSize(14).containsPattern("[A-Z0-9]{12}[0-9]{2}");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "12.ABC.345/01DE-36",
                "00.000.000/E08G-13",
                "12.ABC.345/01DE3A",
                "12.ABC345/01DE-35",
                "00000000000000"
            })
    void rejectsAlteredOrMalformedAlphanumericCnpj(String rawTaxId) {
        assertThatThrownBy(() -> TaxId.of(rawTaxId)).isInstanceOf(InvalidTaxIdException.class);
    }

    @Test
    void doesNotExposeTaxIdInToStringOrValidationFailure() {
        assertThat(TaxId.of("11.222.333/0001-81").toString()).doesNotContain("11222333000181");
        assertThatThrownBy(() -> TaxId.of("52998224724"))
                .isInstanceOf(InvalidTaxIdException.class)
                .hasMessageNotContaining("52998224724");
    }
}
