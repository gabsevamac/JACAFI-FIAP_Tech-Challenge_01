package com.jacafi.tech.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("the JVM default time zone")
class ApplicationTimeZoneTest {

    @Test
    @DisplayName("is UTC after the application enforces it, whatever the machine said before")
    void enforcesUtcOverTheMachineDefault() {

        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/Sao_Paulo")));

        ApplicationTimeZone.enforceUtc();

        assertThat(TimeZone.getDefault().toZoneId().normalized()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("stays UTC when enforced twice")
    void isIdempotent() {
        ApplicationTimeZone.enforceUtc();
        ApplicationTimeZone.enforceUtc();

        assertThat(TimeZone.getDefault().toZoneId().normalized()).isEqualTo(ZoneOffset.UTC);
    }
}
