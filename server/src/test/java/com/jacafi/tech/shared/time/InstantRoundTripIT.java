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

        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/Sao_Paulo")));

        UUID id = save("RTP1A23");

        Instant firstRead = repository.findActiveById(id).orElseThrow().registeredAt();
        Instant secondRead = repository.findActiveById(id).orElseThrow().registeredAt();

        assertThat(secondRead).isEqualTo(firstRead);
    }

    @Test
    @DisplayName("keeps every digit the application clock produced")
    void losesNoPrecisionTheClockProduced() {

        UUID id = save("RTP2B34");

        Instant reloaded = repository.findActiveById(id).orElseThrow().registeredAt();

        assertThat(reloaded.getNano() % 1_000)
                .as("o Clock da aplicacao precisa produzir a precisao que o TIMESTAMPTZ guarda")
                .isZero();
    }
}
