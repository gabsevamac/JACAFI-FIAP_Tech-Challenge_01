package com.jacafi.tech.vehicle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jacafi.tech.vehicle.domain.AuditedOperation;
import com.jacafi.tech.vehicle.domain.DuplicateLicensePlateException;
import com.jacafi.tech.vehicle.domain.InvalidLicensePlateException;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;

class VehicleUseCasesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID CUSTOMER = UUID.fromString("5b6c7d8e-9f0a-4b1c-8d2e-3f4a5b6c7d8e");
    private static final String ACTOR = "advisor@sinates";

    private InMemoryVehicleRepository repository;
    private RecordingAuditTrail auditTrail;
    private RecordingFieldTrail fieldTrail;
    private RegisterVehicleUseCase register;
    private UpdateVehicleUseCase update;
    private RemoveVehicleUseCase remove;
    private FindVehicleUseCase find;
    private ListCustomerVehiclesUseCase list;

    @BeforeEach
    void setUp() {
        repository = new InMemoryVehicleRepository();
        auditTrail = new RecordingAuditTrail();
        fieldTrail = new RecordingFieldTrail();
        register = new RegisterVehicleUseCase(repository, auditTrail, CLOCK);
        update = new UpdateVehicleUseCase(repository, auditTrail, fieldTrail, CLOCK);
        remove = new RemoveVehicleUseCase(repository, auditTrail, CLOCK);
        find = new FindVehicleUseCase(repository);
        list = new ListCustomerVehiclesUseCase(repository);
    }

    private Vehicle registerGol(String plate) {
        return register.register(new RegisterVehicleCommand(plate, "Volkswagen", "Gol", 2020, CUSTOMER, ACTOR));
    }

    @Nested
    class Registration {

        @Test
        void registersAndPersists() {
            Vehicle vehicle = registerGol("ABC1234");

            assertThat(repository.findActiveById(vehicle.getId())).contains(vehicle);
            assertThat(repository.saveCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("normalizes the plate on the way in, so abc-1234 registers as ABC1234")
        void normalizesThePlate() {
            Vehicle vehicle = registerGol("abc-1234");

            assertThat(vehicle.getLicensePlate()).contains(new LicensePlate("ABC1234"));
        }

        @Test
        @DisplayName("a second vehicle with the same plate is a business rule violation")
        void rejectsADuplicatePlate() {
            registerGol("ABC1234");

            assertThatExceptionOfType(DuplicateLicensePlateException.class).isThrownBy(() -> registerGol("ABC1234"));
        }

        @Test
        @DisplayName("the duplicate rule sees through formatting differences")
        void rejectsADuplicateWrittenDifferently() {
            registerGol("ABC1234");

            assertThatExceptionOfType(DuplicateLicensePlateException.class).isThrownBy(() -> registerGol("abc 1234"));
        }

        @Test
        @DisplayName("the conflict message never carries the plate that caused it")
        void doesNotLeakTheConflictingPlate() {
            registerGol("ABC1234");

            assertThatExceptionOfType(DuplicateLicensePlateException.class)
                    .isThrownBy(() -> registerGol("ABC1234"))
                    .withMessageNotContaining("ABC1234");
        }

        @Test
        void rejectsAnInvalidPlateBeforeTouchingTheRepository() {
            assertThatExceptionOfType(InvalidLicensePlateException.class).isThrownBy(() -> registerGol("ABCD123"));

            assertThat(repository.saveCount()).isZero();
        }

        @Test
        @DisplayName("records who registered what, and never the plate")
        void writesAnAuditEntry() {
            Vehicle vehicle = registerGol("ABC1234");

            var entry = auditTrail.only();
            assertThat(entry.vehicleId()).isEqualTo(vehicle.getId());
            assertThat(entry.operation()).isEqualTo(AuditedOperation.REGISTERED);
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.occurredAt()).isEqualTo(CLOCK.instant());
            assertThat(entry.toString()).doesNotContain("ABC1234");
        }

        @Test
        void nothingIsAuditedWhenTheRuleRejectsTheRegistration() {
            registerGol("ABC1234");
            assertThatExceptionOfType(DuplicateLicensePlateException.class).isThrownBy(() -> registerGol("ABC1234"));

            assertThat(auditTrail.entries()).hasSize(1);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesAndAudits() {
            Vehicle vehicle = registerGol("ABC1234");

            Vehicle updated =
                    update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, ACTOR));

            assertThat(updated.getMake()).isEqualTo("Chevrolet");
            assertThat(updated.getModel()).isEqualTo("Onix");
            assertThat(updated.getModelYear()).isEqualTo(2021);
            assertThat(auditTrail.entries()).hasSize(2);
            assertThat(auditTrail.entries().getLast().operation()).isEqualTo(AuditedOperation.UPDATED);
        }

        @Test
        void unknownVehicleIsNotFound() {
            assertThatExceptionOfType(VehicleNotFoundException.class)
                    .isThrownBy(() ->
                            update.update(new UpdateVehicleCommand(UUID.randomUUID(), "Fiat", "Uno", 2010, ACTOR)));
        }

        @Test
        @DisplayName("a removed vehicle is not found, rather than found and refused")
        void removedVehicleIsNotFound() {
            Vehicle vehicle = registerGol("ABC1234");
            remove.remove(vehicle.getId(), ACTOR);

            assertThatExceptionOfType(VehicleNotFoundException.class)
                    .isThrownBy(
                            () -> update.update(new UpdateVehicleCommand(vehicle.getId(), "Fiat", "Uno", 2010, ACTOR)));
        }
    }

    @Nested
    class Removal {

        @Test
        @DisplayName("erases the plate, keeps the record, and reports it in the trail")
        void anonymizesAndKeepsTheRecord() {
            Vehicle vehicle = registerGol("ABC1234");

            remove.remove(vehicle.getId(), ACTOR);

            assertThat(vehicle.getLicensePlate()).isEmpty();
            assertThat(vehicle.isRemoved()).isTrue();
            assertThat(vehicle.getMake()).isEqualTo("Volkswagen");
            assertThat(auditTrail.entries().getLast().operation()).isEqualTo(AuditedOperation.REMOVED);
        }

        @Test
        @DisplayName("a removed vehicle answers no query")
        void disappearsFromQueries() {
            Vehicle vehicle = registerGol("ABC1234");
            remove.remove(vehicle.getId(), ACTOR);

            assertThatExceptionOfType(VehicleNotFoundException.class).isThrownBy(() -> find.byId(vehicle.getId()));
            assertThatExceptionOfType(VehicleNotFoundException.class).isThrownBy(() -> find.byLicensePlate("ABC1234"));
            assertThat(list.list(CUSTOMER, 0, 20).content()).isEmpty();
        }

        @Test
        @DisplayName("the plate is released: the same one can be registered again")
        void releasesThePlateForANewRegistration() {
            Vehicle first = registerGol("ABC1234");
            remove.remove(first.getId(), ACTOR);

            Vehicle second = registerGol("ABC1234");

            assertThat(second.getId()).isNotEqualTo(first.getId());
            assertThat(second.getLicensePlate()).contains(new LicensePlate("ABC1234"));
        }

        @Test
        void removingTwiceIsNotFoundTheSecondTime() {
            Vehicle vehicle = registerGol("ABC1234");
            remove.remove(vehicle.getId(), ACTOR);

            assertThatExceptionOfType(VehicleNotFoundException.class)
                    .isThrownBy(() -> remove.remove(vehicle.getId(), ACTOR));
        }
    }

    @Nested
    class Reads {

        @Test
        void findsByIdAndByPlate() {
            Vehicle vehicle = registerGol("ABC1234");

            assertThat(find.byId(vehicle.getId())).isEqualTo(vehicle);
            assertThat(find.byLicensePlate("abc-1234")).isEqualTo(vehicle);
        }

        @Test
        void unknownIdAndUnknownPlateAreNotFound() {
            assertThatExceptionOfType(VehicleNotFoundException.class).isThrownBy(() -> find.byId(UUID.randomUUID()));
            assertThatExceptionOfType(VehicleNotFoundException.class).isThrownBy(() -> find.byLicensePlate("ZZZ9999"));
        }

        @Test
        @DisplayName("the not-found message for a plate lookup does not echo the plate")
        void doesNotLeakTheSearchedPlate() {
            assertThatExceptionOfType(VehicleNotFoundException.class)
                    .isThrownBy(() -> find.byLicensePlate("ZZZ9999"))
                    .withMessageNotContaining("ZZZ9999");
        }

        @Test
        void pagesTheVehiclesOfACustomer() {
            registerGol("ABC1234");
            registerGol("DEF5678");
            registerGol("GHI9012");

            VehiclePage firstPage = list.list(CUSTOMER, 0, 2);
            VehiclePage secondPage = list.list(CUSTOMER, 1, 2);

            assertThat(firstPage.content()).hasSize(2);
            assertThat(firstPage.totalElements()).isEqualTo(3);
            assertThat(firstPage.totalPages()).isEqualTo(2);
            assertThat(secondPage.content()).hasSize(1);
        }

        @Test
        @DisplayName("a customer with no vehicle gets an empty page, not a not-found")
        void unknownCustomerYieldsAnEmptyPage() {
            VehiclePage page = list.list(UUID.randomUUID(), 0, 20);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
        }
    }
}
