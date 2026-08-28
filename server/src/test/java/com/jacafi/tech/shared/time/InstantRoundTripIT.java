package com.jacafi.tech.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jacafi.tech.support.AbstractIntegrationTest;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

/**
 * An {@code Instant} written to Postgres and read back must be the same instant.
 *
 * <p>What this proves was narrowed after measuring it. The obvious claim — that it guards
 * {@code hibernate.jdbc.time_zone=UTC} — is false: deleting that property leaves both tests green.
 * The reason is that {@code Instant} and {@code TIMESTAMP WITH TIME ZONE} are both absolute
 * points in time, so no zone conversion happens on either side and there is nothing for a zone
 * setting to get wrong.
 *
 * <p>What they do guard, verified by removing it and watching them fail, is the microsecond
 * truncation in {@link TimeConfiguration}.
 *
 * <p>Deliberately does <em>not</em> import {@code FixedClockConfiguration}, unlike the other
 * integration tests. A clock frozen at a whole second has no fractional digits to lose, so under
 * one the truncation assertion would hold no matter what the clock produced — the test would pass
 * and prove nothing. Determinism is the wrong property here: this test needs a clock that really
 * ticks, and asserts a property of the value rather than its identity.
 *
 * <p>The risky combination is the other one: {@code LocalDateTime} against {@code TIMESTAMP
 * WITHOUT TIME ZONE}, where the stored value really does depend on the zone in effect. Nothing in
 * this class detects that, and nothing should — it is a property of the schema rather than of a
 * round trip, and {@code DatabaseMigrationTest} asserts it across every table at once.
 *
 * <p>The vehicle aggregate is used because it is, today, the only persisted type carrying an
 * {@code Instant}. What is under test is the shared configuration, not that slice.
 */
@DisplayName("an Instant through Postgres")
class InstantRoundTripIT extends AbstractIntegrationTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("a1d4e145-e3f8-4fdc-b84e-8584c564c927");

    @Autowired
    private VehicleRepositoryPort repository;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        jdbcTemplate.execute("TRUNCATE TABLE vehicles, audit_trail CASCADE");
    }

    private UUID save(String plate) {
        jdbcTemplate.update("""
                INSERT INTO customers (id, tax_id, name, email, phone, active, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, '52998224725', 'Integration Test Customer', 'integration-test@example.com', '11999999999', TRUE, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)
                ON CONFLICT (id) DO NOTHING
                """, CUSTOMER_ID);
        UUID id = UUID.randomUUID();
        repository.save(
                Vehicle.register(id, new LicensePlate(plate), "Volkswagen", "Gol", 2020, CUSTOMER_ID, clock), "system");
        return id;
    }

    @Test
    @DisplayName("survives unchanged even when the JVM default zone is not UTC")
    void survivesANonUtcJvmDefault() {
        // Sao Paulo rather than UTC so that a green result cannot be explained by the test JVM
        // having been in UTC anyway. This is a regression guard, not a proof: it is what would
        // catch someone remapping registered_at to a column WITHOUT time zone, which is the case
        // where the platform zone starts deciding what gets stored.
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/Sao_Paulo")));

        UUID id = save("RTP1A23");

        Instant firstRead = repository.findActiveById(id).orElseThrow().registeredAt();
        Instant secondRead = repository.findActiveById(id).orElseThrow().registeredAt();

        assertThat(secondRead).isEqualTo(firstRead);
    }

    @Test
    @DisplayName("keeps every digit the application clock produced")
    void losesNoPrecisionTheClockProduced() {
        // The load-bearing test of this class. Clock.systemUTC() resolves to nanoseconds, Postgres
        // TIMESTAMPTZ stores microseconds, and the three surplus digits were dropped on write
        // without a word — so a POST response and a later GET of the same resource disagreed on a
        // field nobody had changed. Verified by reverting the truncation and watching this fail.
        UUID id = save("RTP2B34");

        Instant reloaded = repository.findActiveById(id).orElseThrow().registeredAt();

        assertThat(reloaded.getNano() % 1_000)
                .as("o Clock da aplicacao precisa produzir a precisao que o TIMESTAMPTZ guarda")
                .isZero();
    }
}
