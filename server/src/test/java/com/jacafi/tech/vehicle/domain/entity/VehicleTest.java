package com.jacafi.tech.vehicle.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class VehicleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void keepsThePersistenceVersionAndRefusesChangesAfterLogicalRemoval() {
        Vehicle vehicle = Vehicle.restore(
                UUID.randomUUID(),
                new LicensePlate("ABC1D23"),
                "Volkswagen",
                "Gol",
                2020,
                CUSTOMER_ID,
                4,
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2026-08-26T00:00:00Z"),
                null);

        vehicle.remove(CLOCK.instant());

        assertThat(vehicle.version()).isEqualTo(4);
        assertThat(vehicle.active()).isFalse();
        assertThat(vehicle.removedAt()).contains(CLOCK.instant());
        assertThatIllegalStateException().isThrownBy(() -> vehicle.changeDetails("Ford", "Ka", 2020, CLOCK));
    }
}
