package com.jacafi.tech.vehicle.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.vehicle.domain.exception.InvalidLicensePlateException;

class LicensePlateTest {

    @Test
    void normalizesBrazilianPlateFormatsBeforeValidation() {
        assertThat(new LicensePlate(" abc-1234 ").value()).isEqualTo("ABC1234");
        assertThat(new LicensePlate("abc1d23").value()).isEqualTo("ABC1D23");
    }

    @Test
    void rejectsAnInvalidPlateWithoutEchoingIt() {
        assertThatThrownBy(() -> new LicensePlate("BAD98765"))
                .isInstanceOf(InvalidLicensePlateException.class)
                .hasMessageNotContaining("BAD98765");
    }
}
