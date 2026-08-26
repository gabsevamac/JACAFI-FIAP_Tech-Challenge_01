package com.jacafi.tech.shared.time;

import java.time.Clock;
import java.time.Duration;

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
 * time zone of whichever machine happens to be running the application. {@link
 * ApplicationTimeZone} pins that default as well, so the two agree even for code that reads the
 * platform zone without asking.
 *
 * <p>Named for the subject rather than for the bean because it is the home of the slice's other
 * time concerns as they arrive — the auditing {@code DateTimeProvider} reads this same clock,
 * and a second {@code Clock} bean declared elsewhere would break injection by ambiguity.
 */
@Configuration
public class TimeConfiguration {

    /**
     * Truncated to microseconds, which is the precision Postgres {@code TIMESTAMPTZ} actually
     * stores.
     *
     * <p>{@code Clock.systemUTC()} resolves to nanoseconds, and the extra three digits do not
     * survive a write. Measured on the vehicle endpoint, the same field of the same resource came
     * back two different ways:
     *
     * <pre>
     *   POST response   2026-08-26T14:20:48.492948227Z   (from the clock, in memory)
     *   GET  response   2026-08-26T14:20:48.492948Z      (after the round trip)
     * </pre>
     *
     * <p>A client that stores the creation response and compares it against a later read finds
     * them unequal, for a resource nobody changed. The truncation is silent — no error, no
     * warning, just three digits gone.
     *
     * <p>Rounding at the source removes the discrepancy instead of documenting it: what the
     * application produces is now exactly what the database can hold. The precision lost is
     * precision that was never persisted anyway.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
