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
        // Sao Paulo on purpose, and not merely "something else": it is where this group runs the
        // application, it is three hours from UTC, and it observed daylight saving until 2019 —
        // so a bug that survives here is a bug that shows up in the group's own logs first.
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

    // No teardown restoring the previous default, deliberately. UTC is what the application runs
    // in, so leaving the JVM in UTC leaves the remaining tests closer to production, not further
    // from it. Restoring America/Sao_Paulo here would be restoring the wrong thing.
}
