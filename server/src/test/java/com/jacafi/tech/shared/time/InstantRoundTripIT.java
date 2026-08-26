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
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

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
 * truncation in {@link TimeConfiguration}. Restore {@code Clock.systemUTC()} and both fail with
 * {@code expected 2026-08-26T14:25:52.291324069Z but was 2026-08-26T14:25:52.291324Z}.
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

    @Autowired
    private VehicleRepository repository;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        jdbcTemplate.execute("TRUNCATE TABLE vehicles, vehicle_audit_entries");
    }

    @Test
    @DisplayName("survives unchanged even when the JVM default zone is not UTC")
    void survivesANonUtcJvmDefault() {
        // Sao Paulo rather than UTC so that a green result cannot be explained by the test JVM
        // having been in UTC anyway. This is a regression guard, not a proof: it is what would
        // catch someone remapping registered_at to a column WITHOUT time zone, which is the case
        // where the platform zone starts deciding what gets stored.
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/Sao_Paulo")));

        Instant registeredAt = clock.instant();
        UUID id = UUID.randomUUID();

        repository.save(Vehicle.builder()
                .id(id)
                .licensePlate(new LicensePlate("RTP1A23"))
                .make("Volkswagen")
                .model("Gol")
                .modelYear(2020)
                .customerId(UUID.randomUUID())
                .registeredAt(registeredAt)
                .updatedAt(registeredAt)
                .restore());

        Vehicle reloaded = repository.findActiveById(id).orElseThrow();

        assertThat(reloaded.getRegisteredAt()).isEqualTo(registeredAt);
    }

    @Test
    @DisplayName("keeps every digit the application clock produced")
    void losesNoPrecisionTheClockProduced() {
        // The load-bearing test of this class. Clock.systemUTC() resolves to nanoseconds, Postgres
        // TIMESTAMPTZ stores microseconds, and the three surplus digits were dropped on write
        // without a word — so a POST response and a later GET of the same resource disagreed on a
        // field nobody had changed. Verified by reverting the truncation and watching this fail.
        Instant registeredAt = clock.instant();
        UUID id = UUID.randomUUID();

        repository.save(Vehicle.builder()
                .id(id)
                .licensePlate(new LicensePlate("RTP2B34"))
                .make("Fiat")
                .model("Uno")
                .modelYear(2021)
                .customerId(UUID.randomUUID())
                .registeredAt(registeredAt)
                .updatedAt(registeredAt)
                .restore());

        Instant reloaded = repository.findActiveById(id).orElseThrow().getRegisteredAt();

        assertThat(reloaded).isEqualTo(registeredAt);
        assertThat(reloaded.getNano() % 1_000).isZero();
    }
}
