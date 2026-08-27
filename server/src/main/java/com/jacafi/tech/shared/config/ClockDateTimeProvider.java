package com.jacafi.tech.shared.config;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.auditing.DateTimeProvider;

/**
 * Feeds Spring Data auditing from the application {@link Clock} instead of the system clock.
 *
 * <p>Without this, {@code @CreatedDate} calls {@code Instant.now()} internally and the audited
 * timestamps become the one part of the application that ignores the injected clock. The cost is
 * not philosophical: an integration test can then only assert that {@code createdAt} is
 * approximately now, which passes on a fast machine and fails on a loaded runner.
 *
 * <p>Returning {@link Instant} rather than {@code LocalDateTime} matters too — it is what keeps
 * the audited columns absolute, and therefore independent of the zone of whichever machine wrote
 * the row.
 */
public class ClockDateTimeProvider implements DateTimeProvider {

    private final Clock clock;

    public ClockDateTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(Instant.now(clock));
    }
}
