package com.jacafi.tech.shared.config;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.auditing.DateTimeProvider;

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
