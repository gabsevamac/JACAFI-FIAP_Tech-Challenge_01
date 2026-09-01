package com.jacafi.tech.vehicle.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.DuplicateLicensePlateException;

class VehicleServicesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID CUSTOMER_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final PageQuery PAGE = new PageQuery(0, 20, List.of(SortCriterion.ascending("id")));

    @Test
    void operationalUserRegistersAnActiveVehicleAndRecordsOneBusinessAction() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        RecordedAuditTrail trail = new RecordedAuditTrail();
        RegisterVehicleService service = new RegisterVehicleService(vehicles, trail, operationalAccess(), CLOCK);

        Vehicle vehicle = service.register("abc-1234", "Volkswagen", "Gol", 2020, CUSTOMER_A_ID);

        assertThat(vehicle.licensePlate().value()).isEqualTo("ABC1234");
        assertThat(trail.events()).singleElement().satisfies(event -> {
            assertThat(event.aggregateType()).isEqualTo("Vehicle");
            assertThat(event.action()).isEqualTo("REGISTERED");
            assertThat(event.actor()).isEqualTo("employee");
        });
    }

    @Test
    void customerListUsesOnlyTheCustomerIdentifierFromTheAuthenticatedPrincipal() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        vehicles.save(vehicle("ABC1D23", CUSTOMER_A_ID), "employee");
        vehicles.save(vehicle("DEF2G34", CUSTOMER_B_ID), "employee");
        ListCurrentCustomerVehiclesService service =
                new ListCurrentCustomerVehiclesService(vehicles, customerAccess(CUSTOMER_A_ID));

        PageResult<Vehicle> page = service.list(PAGE);

        assertThat(page.content()).extracting(Vehicle::customerId).containsExactly(CUSTOMER_A_ID);
        assertThat(vehicles.lastListedCustomerId()).isEqualTo(CUSTOMER_A_ID);
    }

    @Test
    void customerCannotUseOperationalVehicleCommands() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        FindVehicleService service = new FindVehicleService(vehicles, customerAccess(CUSTOMER_A_ID));

        assertThatThrownBy(() -> service.findById(UUID.randomUUID())).isInstanceOf(AccountAccessDeniedException.class);
        assertThat(vehicles.lookupCount()).isZero();
    }

    @Test
    void duplicateActivePlateIsRejectedBeforeRegistration() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        vehicles.save(vehicle("ABC1D23", CUSTOMER_A_ID), "employee");
        RegisterVehicleService service =
                new RegisterVehicleService(vehicles, new RecordedAuditTrail(), operationalAccess(), CLOCK);

        assertThatThrownBy(() -> service.register("abc1d23", "Ford", "Ka", 2020, CUSTOMER_B_ID))
                .isInstanceOf(DuplicateLicensePlateException.class);
    }

    @Test
    void successfulUpdateRecordsOneUpdatedAction() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        Vehicle vehicle = vehicle("ABC1D23", CUSTOMER_A_ID);
        vehicles.save(vehicle, "employee");
        RecordedAuditTrail trail = new RecordedAuditTrail();

        new UpdateVehicleService(vehicles, trail, operationalAccess(), CLOCK).update(vehicle.id(), "Ford", "Ka", 2020);

        assertThat(trail.events())
                .singleElement()
                .satisfies(event -> assertThat(event.action()).isEqualTo("UPDATED"));
    }

    @Test
    void removalUsesTheAuthenticatedActorForLogicalDeletionAndAudit() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        Vehicle vehicle = vehicle("ABC1D23", CUSTOMER_A_ID);
        vehicles.save(vehicle, "employee");
        RecordedAuditTrail trail = new RecordedAuditTrail();

        new RemoveVehicleService(vehicles, trail, operationalAccess(), CLOCK).remove(vehicle.id());

        assertThat(vehicle.active()).isFalse();
        assertThat(vehicles.lastSaveActor()).isEqualTo("employee");
        assertThat(trail.events()).singleElement().satisfies(event -> {
            assertThat(event.action()).isEqualTo("REMOVED");
            assertThat(event.actor()).isEqualTo("employee");
        });
    }

    @Test
    void failedPersistenceDoesNotProduceAnAuditEvent() {
        InMemoryVehicles vehicles = new InMemoryVehicles();
        vehicles.failSaves();
        RecordedAuditTrail trail = new RecordedAuditTrail();

        assertThatThrownBy(() -> new RegisterVehicleService(vehicles, trail, operationalAccess(), CLOCK)
                        .register("ABC1D23", "Volkswagen", "Gol", 2020, CUSTOMER_A_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(trail.events()).isEmpty();
    }

    private static VehicleAccessPolicy operationalAccess() {
        return new VehicleAccessPolicy(user("employee", Set.of(Role.EMPLOYEE), null));
    }

    private static VehicleAccessPolicy customerAccess(UUID customerId) {
        return new VehicleAccessPolicy(user("customer", Set.of(Role.CUSTOMER), customerId));
    }

    private static CurrentAuthenticatedUserPort user(String username, Set<Role> roles, UUID customerId) {
        return () -> new AuthenticatedUser(UUID.randomUUID().toString(), username, roles, customerId);
    }

    private static Vehicle vehicle(String plate, UUID customerId) {
        return Vehicle.register(
                UUID.randomUUID(), new LicensePlate(plate), "Volkswagen", "Gol", 2020, customerId, CLOCK);
    }

    private static final class RecordedAuditTrail implements AuditTrailPort {

        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }

        List<AuditEvent> events() {
            return events;
        }
    }

    private static final class InMemoryVehicles implements VehicleRepositoryPort {

        private final Map<UUID, Vehicle> storage = new LinkedHashMap<>();
        private UUID lastListedCustomerId;
        private int lookupCount;
        private String lastSaveActor;
        private boolean failSaves;

        @Override
        public Vehicle save(Vehicle vehicle, String actor) {
            if (failSaves) {
                throw new IllegalStateException("persistence unavailable");
            }
            storage.put(vehicle.id(), vehicle);
            lastSaveActor = actor;
            return vehicle;
        }

        @Override
        public boolean existsActiveByLicensePlate(LicensePlate licensePlate) {
            return storage.values().stream()
                    .anyMatch(vehicle ->
                            vehicle.active() && vehicle.licensePlate().equals(licensePlate));
        }

        @Override
        public Optional<Vehicle> findActiveById(UUID vehicleId) {
            lookupCount++;
            return Optional.ofNullable(storage.get(vehicleId)).filter(Vehicle::active);
        }

        @Override
        public Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate) {
            lookupCount++;
            return storage.values().stream()
                    .filter(Vehicle::active)
                    .filter(vehicle -> vehicle.licensePlate().equals(licensePlate))
                    .findFirst();
        }

        @Override
        public PageResult<Vehicle> findActiveByCustomerId(UUID customerId, PageQuery query) {
            lastListedCustomerId = customerId;
            List<Vehicle> result = storage.values().stream()
                    .filter(Vehicle::active)
                    .filter(vehicle -> vehicle.customerId().equals(customerId))
                    .toList();
            return PageResult.of(result, query.page(), query.size(), result.size());
        }

        UUID lastListedCustomerId() {
            return lastListedCustomerId;
        }

        int lookupCount() {
            return lookupCount;
        }

        String lastSaveActor() {
            return lastSaveActor;
        }

        void failSaves() {
            failSaves = true;
        }
    }
}
