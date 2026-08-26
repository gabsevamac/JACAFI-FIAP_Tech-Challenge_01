package com.jacafi.tech.vehicle.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jacafi.tech.support.AbstractIntegrationTest;
import com.jacafi.tech.vehicle.domain.DuplicateLicensePlateException;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Exercises the partial unique index directly, bypassing the application layer's check.
 *
 * <p>This is the reason the integration tests need a real Postgres. H2 does not implement a unique
 * index with a predicate, so on H2 either the constraint would not exist — and the first test here
 * would pass while proving nothing — or a plain unique index would exist and the plate-reuse test
 * would fail for the wrong reason.
 */
class VehicleRepositoryAdapterIT extends AbstractIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);
    private static final LicensePlate PLATE = new LicensePlate("ABC1D23");

    @Autowired
    private VehicleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE TABLE vehicles, vehicle_audit_entries");
    }

    private Vehicle vehicleWith(LicensePlate plate) {
        return Vehicle.builder()
                .id(UUID.randomUUID())
                .licensePlate(plate)
                .make("Volkswagen")
                .model("Gol")
                .modelYear(2020)
                .customerId(UUID.randomUUID())
                .register(CLOCK);
    }

    @Test
    @DisplayName("the database refuses a second active vehicle with the same plate")
    void enforcesUniquenessAmongActiveVehicles() {
        repository.save(vehicleWith(PLATE));

        // Straight at the adapter, so the application layer's check is not in the way: this is the
        // concurrent case, where two registrations both passed that check.
        assertThatExceptionOfType(DuplicateLicensePlateException.class)
                .isThrownBy(() -> repository.save(vehicleWith(PLATE)));
    }

    @Test
    @DisplayName("the same plate is accepted again once the previous vehicle was removed")
    void releasesThePlateAfterRemoval() {
        Vehicle first = vehicleWith(PLATE);
        repository.save(first);

        first.remove(CLOCK);
        repository.save(first);

        Vehicle second = vehicleWith(PLATE);
        repository.save(second);

        assertThat(repository.findActiveByLicensePlate(PLATE)).contains(second);
    }

    @Test
    @DisplayName("a removed row keeps its history and stores a token in place of the plate")
    void storesAnIrreversibleTokenAfterRemoval() {
        Vehicle vehicle = vehicleWith(PLATE);
        repository.save(vehicle);
        vehicle.remove(CLOCK);
        repository.save(vehicle);

        String storedPlate = jdbcTemplate.queryForObject(
                "SELECT license_plate FROM vehicles WHERE id = ?", String.class, vehicle.getId());

        assertThat(storedPlate).doesNotContain(PLATE.value()).isEqualTo("ANON-" + vehicle.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT make FROM vehicles WHERE id = ?", String.class, vehicle.getId()))
                .isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("a removed vehicle is invisible to every lookup")
    void removedVehiclesAnswerNoLookup() {
        Vehicle vehicle = vehicleWith(PLATE);
        repository.save(vehicle);
        vehicle.remove(CLOCK);
        repository.save(vehicle);

        assertThat(repository.findActiveById(vehicle.getId())).isEmpty();
        assertThat(repository.findActiveByLicensePlate(PLATE)).isEmpty();
        assertThat(repository.existsActiveWithLicensePlate(PLATE)).isFalse();
    }

    @Test
    @DisplayName("a saved aggregate comes back with every attribute intact")
    void roundTripsTheAggregate() {
        Vehicle saved = vehicleWith(PLATE);
        repository.save(saved);

        Vehicle loaded = repository.findActiveById(saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getLicensePlate()).contains(PLATE);
        assertThat(loaded.getMake()).isEqualTo("Volkswagen");
        assertThat(loaded.getModel()).isEqualTo("Gol");
        assertThat(loaded.getModelYear()).isEqualTo(2020);
        assertThat(loaded.getCustomerId()).isEqualTo(saved.getCustomerId());
        assertThat(loaded.getRegisteredAt()).isEqualTo(saved.getRegisteredAt());
        assertThat(loaded.isRemoved()).isFalse();
    }
}
