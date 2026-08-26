package com.jacafi.tech.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("one recorded field change")
class FieldChangeTest {

    private static final Instant AT = Instant.parse("2026-01-15T10:30:00Z");

    private static FieldChange change(String oldValue, String newValue, String reason) {
        return new FieldChange("Vehicle", UUID.randomUUID(), "make", oldValue, newValue, reason, AT, "advisor");
    }

    @Test
    @DisplayName("refuses to record a change that changes nothing")
    void refusesANonChange() {
        // Entradas com valor antigo igual ao novo tornariam "quando isto mudou pela ultima vez"
        // irrespondivel pela leitura da linha mais recente.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> change("Volkswagen", "Volkswagen", null))
                .withMessageContaining("must change");
    }

    @Test
    @DisplayName("refuses two nulls, which is also a non-change")
    void refusesTwoNulls() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> change(null, null, null));
    }

    @Test
    @DisplayName("accepts a null on either side, because a field can gain or lose a value")
    void acceptsOneSidedNulls() {
        assertThatNoException().isThrownBy(() -> change(null, "Volkswagen", null));
        assertThatNoException().isThrownBy(() -> change("Volkswagen", null, null));
    }

    @ParameterizedTest
    @DisplayName("refuses a blank reason, which is not the same as no reason")
    @ValueSource(strings = {"", "   "})
    void refusesABlankReason(String reason) {
        // Nulo e o caso de uso declarando que nao tem motivo a registrar; branco e um campo de
        // formulario que ninguem preencheu. HS9 mantem a semantica do motivo em aberto, e essa
        // distincao e o que preserva o que era sabido na epoca.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> change("Volkswagen", "Fiat", reason))
                .withMessageContaining("reason");
    }

    @Test
    @DisplayName("accepts a null reason")
    void acceptsANullReason() {
        assertThat(change("Volkswagen", "Fiat", null).reason()).isNull();
    }

    @Test
    @DisplayName("refuses to exist without aggregate, field, moment or author")
    void refusesMissingIdentity() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FieldChange("Vehicle", null, "make", "a", "b", null, AT, "advisor"));
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new FieldChange("Vehicle", UUID.randomUUID(), "make", "a", "b", null, null, "advisor"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new FieldChange(" ", UUID.randomUUID(), "make", "a", "b", null, AT, "advisor"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new FieldChange("Vehicle", UUID.randomUUID(), "", "a", "b", null, AT, "advisor"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new FieldChange("Vehicle", UUID.randomUUID(), "make", "a", "b", null, AT, " "));
    }
}
