package com.jacafi.tech.vehicle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class VehicleTest {

    /** 2026-08-25T12:00:00Z. Fixed so that the model year bounds are the same on every run. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);
    private static final int CURRENT_YEAR = 2026;

    private static final UUID ID = UUID.fromString("0f7c9a1e-3b2d-4c5f-8a9b-1d2e3f4a5b6c");
    private static final CustomerId CUSTOMER = new CustomerId(
            UUID.fromString("5b6c7d8e-9f0a-4b1c-8d2e-3f4a5b6c7d8e"));
    private static final LicensePlate PLATE = new LicensePlate("ABC1234");

    private static Vehicle register() {
        return Vehicle.register(ID, PLATE, "Volkswagen", "Gol", 2020, CUSTOMER, CLOCK);
    }

    @Nested
    class Registration {

        @Test
        @DisplayName("keeps every attribute it was given and starts out active")
        void registersWithTheGivenAttributes() {
            Vehicle vehicle = register();

            assertThat(vehicle.getId()).isEqualTo(ID);
            assertThat(vehicle.getLicensePlate()).contains(PLATE);
            assertThat(vehicle.getMake()).isEqualTo("Volkswagen");
            assertThat(vehicle.getModel()).isEqualTo("Gol");
            assertThat(vehicle.getModelYear()).isEqualTo(2020);
            assertThat(vehicle.getCustomerId()).isEqualTo(CUSTOMER);
            assertThat(vehicle.getRegisteredAt()).isEqualTo(CLOCK.instant());
            assertThat(vehicle.getUpdatedAt()).isEqualTo(CLOCK.instant());
            assertThat(vehicle.isRemoved()).isFalse();
            assertThat(vehicle.getRemovedAt()).isEmpty();
        }

        @Test
        @DisplayName("trims and collapses whitespace in make and model")
        void normalizesText() {
            Vehicle vehicle = Vehicle.register(ID, PLATE, "  Volks   wagen ", " Gol  GTI ", 2020, CUSTOMER, CLOCK);

            assertThat(vehicle.getMake()).isEqualTo("Volks wagen");
            assertThat(vehicle.getModel()).isEqualTo("Gol GTI");
        }

        @Test
        void rejectsBlankMake() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Vehicle.register(ID, PLATE, "   ", "Gol", 2020, CUSTOMER, CLOCK))
                    .withMessageContaining("make");
        }

        @Test
        void rejectsBlankModel() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Vehicle.register(ID, PLATE, "Volkswagen", "", 2020, CUSTOMER, CLOCK))
                    .withMessageContaining("model");
        }

        @Test
        @DisplayName("every vehicle references a customer")
        void rejectsMissingCustomer() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Vehicle.register(ID, PLATE, "Volkswagen", "Gol", 2020, null, CLOCK));
        }

        @Test
        void rejectsMissingLicensePlate() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Vehicle.register(ID, null, "Volkswagen", "Gol", 2020, CUSTOMER, CLOCK));
        }
    }

    @Nested
    @DisplayName("model year range, at the boundaries")
    class ModelYearRange {

        @Test
        void accepts1900() {
            assertThat(Vehicle.register(ID, PLATE, "Ford", "T", 1900, CUSTOMER, CLOCK).getModelYear())
                    .isEqualTo(1900);
        }

        @Test
        void rejects1899() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Vehicle.register(ID, PLATE, "Ford", "T", 1899, CUSTOMER, CLOCK));
        }

        @Test
        @DisplayName("accepts next year, because next year's models are sold this year")
        void acceptsNextYear() {
            assertThat(Vehicle.register(ID, PLATE, "Fiat", "Argo", CURRENT_YEAR + 1, CUSTOMER, CLOCK)
                    .getModelYear()).isEqualTo(CURRENT_YEAR + 1);
        }

        @Test
        void rejectsTwoYearsAhead() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Vehicle.register(ID, PLATE, "Fiat", "Argo", CURRENT_YEAR + 2, CUSTOMER, CLOCK));
        }
    }

    @Nested
    class Update {

        @Test
        @DisplayName("replaces the descriptive attributes and moves updatedAt")
        void updatesAttributes() {
            Vehicle vehicle = register();
            Clock later = Clock.fixed(Instant.parse("2026-09-01T08:30:00Z"), ZoneOffset.UTC);

            vehicle.update("Chevrolet", "Onix", 2021, later);

            assertThat(vehicle.getMake()).isEqualTo("Chevrolet");
            assertThat(vehicle.getModel()).isEqualTo("Onix");
            assertThat(vehicle.getModelYear()).isEqualTo(2021);
            assertThat(vehicle.getUpdatedAt()).isEqualTo(later.instant());
            assertThat(vehicle.getRegisteredAt()).isEqualTo(CLOCK.instant());
        }

        @Test
        @DisplayName("leaves the license plate untouched: it is the business identity")
        void neverChangesThePlate() {
            Vehicle vehicle = register();

            vehicle.update("Chevrolet", "Onix", 2021, CLOCK);

            assertThat(vehicle.getLicensePlate()).contains(PLATE);
        }

        @Test
        void appliesTheSameModelYearRange() {
            Vehicle vehicle = register();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> vehicle.update("Chevrolet", "Onix", 1899, CLOCK));
        }

        @Test
        void rejectsUpdatingARemovedVehicle() {
            Vehicle vehicle = register();
            vehicle.remove(CLOCK);

            assertThatIllegalStateException()
                    .isThrownBy(() -> vehicle.update("Chevrolet", "Onix", 2021, CLOCK));
        }
    }

    @Nested
    class Removal {

        @Test
        @DisplayName("erases the plate, records the moment and keeps the rest of the record")
        void erasesThePlateAndKeepsTheRecord() {
            Vehicle vehicle = register();
            Clock later = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC);

            vehicle.remove(later);

            assertThat(vehicle.getLicensePlate()).isEmpty();
            assertThat(vehicle.isRemoved()).isTrue();
            assertThat(vehicle.getRemovedAt()).contains(later.instant());
            // The service history depends on these surviving (LGPD Art. 16 I).
            assertThat(vehicle.getMake()).isEqualTo("Volkswagen");
            assertThat(vehicle.getModel()).isEqualTo("Gol");
            assertThat(vehicle.getCustomerId()).isEqualTo(CUSTOMER);
            assertThat(vehicle.getRegisteredAt()).isEqualTo(CLOCK.instant());
        }

        @Test
        void cannotBeRemovedTwice() {
            Vehicle vehicle = register();
            vehicle.remove(CLOCK);

            assertThatIllegalStateException().isThrownBy(() -> vehicle.remove(CLOCK));
        }
    }

    @Nested
    class Identity {

        @Test
        @DisplayName("two vehicles with the same id are the same vehicle")
        void equalsById() {
            Vehicle one = register();
            Vehicle other = Vehicle.register(ID, new LicensePlate("XYZ9K87"), "Fiat", "Uno", 2010, CUSTOMER, CLOCK);

            assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
        }

        @Test
        void differentIdsAreDifferentVehicles() {
            assertThat(register()).isNotEqualTo(
                    Vehicle.register(UUID.randomUUID(), PLATE, "Volkswagen", "Gol", 2020, CUSTOMER, CLOCK));
        }
    }

    @Nested
    @DisplayName("restoring from storage")
    class Restore {

        @Test
        @DisplayName("rebuilds a removed vehicle, a state registration can never produce")
        void rebuildsARemovedVehicle() {
            Instant registeredAt = Instant.parse("2020-01-15T09:00:00Z");
            Instant removedAt = Instant.parse("2026-03-20T14:00:00Z");

            Vehicle vehicle = Vehicle.restore(ID, null, "Volkswagen", "Gol", 2020, CUSTOMER,
                    registeredAt, removedAt, removedAt);

            assertThat(vehicle.getLicensePlate()).isEmpty();
            assertThat(vehicle.isRemoved()).isTrue();
            assertThat(vehicle.getRemovedAt()).contains(removedAt);
        }

        @Test
        @DisplayName("does not re-apply the model year rule, so old rows stay readable")
        void doesNotRevalidateBusinessRules() {
            Instant registeredAt = Instant.parse("2020-01-15T09:00:00Z");

            Vehicle vehicle = Vehicle.restore(ID, PLATE, "Ford", "A", 1850, CUSTOMER,
                    registeredAt, registeredAt, null);

            assertThat(vehicle.getModelYear()).isEqualTo(1850);
        }
    }

    @Test
    @DisplayName("toString never carries the full plate")
    void toStringIsMasked() {
        Vehicle vehicle = register();

        assertThat(vehicle.toString())
                .doesNotContain("ABC1234")
                .contains("ABC***4");
    }

    @Test
    @DisplayName("toString of a removed vehicle shows no plate at all")
    void toStringOfRemovedVehicle() {
        Vehicle vehicle = register();
        vehicle.remove(CLOCK);

        assertThat(vehicle.toString()).doesNotContain("ABC").contains("<erased>");
    }
}
