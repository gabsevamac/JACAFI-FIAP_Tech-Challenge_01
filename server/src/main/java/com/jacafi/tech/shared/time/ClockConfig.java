package com.jacafi.tech.shared.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of time for the whole application.
 *
 * <p>Cross-cutting on purpose, and the one place it can safely live. Domain code takes a
 * {@link Clock} as a parameter so its date rules are deterministic under test; that only works if
 * production code has one clock to inject. Were each slice to declare its own bean, injection
 * would fail by ambiguity the moment the second slice did it — a guaranteed integration bug, not
 * a hypothetical one.
 *
 * <p>UTC, not the system default zone: stored instants and comparisons must not depend on the
 * time zone of whichever machine happens to be running the application.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
