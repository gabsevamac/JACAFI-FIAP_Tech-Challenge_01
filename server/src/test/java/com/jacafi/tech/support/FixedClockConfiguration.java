package com.jacafi.tech.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the application clock with one that does not move.
 *
 * <p>Import it from a test that needs to assert on a timestamp:
 *
 * <pre>{@code
 * @Import(FixedClockConfiguration.class)
 * class SomethingIT extends AbstractIntegrationTest { ... }
 * }</pre>
 *
 * <p>Without it, asserting that a record was audited means asserting that {@code createdAt} is
 * "roughly now", which is the shape of a test that passes on a fast machine and fails on a loaded
 * CI runner. With a fixed instant the assertion is an equality, and equality does not flake.
 *
 * <p>{@code @Primary} rather than bean overriding: the production {@link Clock} bean stays in the
 * context, and this one wins injection. Overriding by name requires
 * {@code spring.main.allow-bean-definition-overriding}, which would silently permit accidental
 * overrides everywhere else.
 */
@TestConfiguration
public class FixedClockConfiguration {

    /**
     * A Thursday, mid-morning, UTC. Arbitrary, and deliberately not "now" — a test that reads
     * correctly only on the day it was written is a test that will be deleted rather than fixed.
     */
    public static final Instant FIXED_INSTANT = Instant.parse("2026-01-15T10:30:00Z");

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
