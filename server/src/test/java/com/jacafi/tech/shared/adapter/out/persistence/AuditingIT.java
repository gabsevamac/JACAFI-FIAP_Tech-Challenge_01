package com.jacafi.tech.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.jacafi.tech.support.AbstractIntegrationTest;
import com.jacafi.tech.support.FixedClockConfiguration;
import com.jacafi.tech.vehicle.application.RegisterVehicleCommand;
import com.jacafi.tech.vehicle.application.RegisterVehicleUseCase;
import com.jacafi.tech.vehicle.application.RemoveVehicleUseCase;
import com.jacafi.tech.vehicle.application.UpdateVehicleCommand;
import com.jacafi.tech.vehicle.application.UpdateVehicleUseCase;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Both halves of auditing, over the real stack.
 *
 * <p>The vehicle slice is the subject because it is the one with a complete use case layer. What is
 * under test is the shared machinery: {@code AuditableJpaEntity}, the auditor and the clock-backed
 * date provider, and the shared field-level trail.
 */
@Import(FixedClockConfiguration.class)
// O autor auditado vem do SecurityContext, e nao do actor que viaja no comando. Sao duas coisas
// distintas de proposito: o comando carrega quem a camada web disse ser o ator, o AuditorAware
// carrega quem o filtro de seguranca autenticou. Num request real coincidem; num teste que chama
// o caso de uso direto, so o segundo prova que o AuditorAware funciona.
@WithMockUser(username = AuditingIT.ACTOR)
@DisplayName("auditing")
class AuditingIT extends AbstractIntegrationTest {

    static final String ACTOR = "advisor@sinates";

    @Autowired
    private RegisterVehicleUseCase register;

    @Autowired
    private UpdateVehicleUseCase update;

    @Autowired
    private RemoveVehicleUseCase remove;

    @Autowired
    private VehicleRepository repository;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE vehicles, vehicle_audit_entries, audit_trail");
    }

    private Vehicle register(String plate) {
        return register.register(
                new RegisterVehicleCommand(plate, "Volkswagen", "Gol", 2020, UUID.randomUUID(), ACTOR));
    }

    private Map<String, Object> rowOf(UUID id) {
        return jdbc.queryForMap("""
                SELECT created_at, created_by, updated_at, updated_by, deleted_at, deleted_by, version
                FROM vehicles WHERE id = ?
                """, id);
    }

    @Nested
    @DisplayName("technical columns")
    class Technical {

        @Test
        @DisplayName("are filled on insert, from the injected clock and not the system clock")
        void fillsCreationColumns() {
            Vehicle vehicle = register("AUD1A11");

            Map<String, Object> row = rowOf(vehicle.getId());

            // An equality, not a tolerance. That is the entire point of routing auditing through
            // the Clock bean: with the system clock this could only assert "roughly now", which
            // passes on a fast machine and fails on a loaded runner.
            assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant())
                    .isEqualTo(FixedClockConfiguration.FIXED_INSTANT);
            assertThat(row.get("created_by")).isEqualTo(ACTOR);
            assertThat(((java.sql.Timestamp) row.get("updated_at")).toInstant())
                    .isEqualTo(FixedClockConfiguration.FIXED_INSTANT);
            assertThat(row.get("updated_by")).isEqualTo(ACTOR);
            assertThat(row.get("deleted_at")).isNull();
        }

        @Test
        @DisplayName("record the author of the last write, which overwrites the previous one")
        @WithMockUser(username = "manager@sinates")
        void updatesTheModificationColumns() {
            Vehicle vehicle = register("AUD2B22");

            update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, "manager@sinates"));

            Map<String, Object> row = rowOf(vehicle.getId());
            assertThat(row.get("updated_by")).isEqualTo("manager@sinates");
        }

        @Test
        @DisplayName("fall back to \"system\" when nothing is authenticated")
        @WithAnonymousUser
        void attributesUnauthenticatedWritesToSystem() {
            // A migration, a scheduled job, a seed. Never null and never blank: a nullable author
            // column forces every report on authorship to decide what null means, and the usual
            // decision is to omit the row — which turns an unattributed write into an invisible
            // one. Note that @WithAnonymousUser is the harder case: Spring installs an
            // AnonymousAuthenticationToken that answers isAuthenticated() with true, and naming it
            // would attribute the write to a user called "anonymousUser".
            Vehicle vehicle = register("AUD8H88");

            assertThat(rowOf(vehicle.getId()).get("created_by")).isEqualTo("system");
        }

        @Test
        @DisplayName("start optimistic locking at zero and advance it on write")
        void advancesTheVersion() {
            Vehicle vehicle = register("AUD3C33");
            assertThat((Number) rowOf(vehicle.getId()).get("version")).isEqualTo(0L);

            update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, ACTOR));

            assertThat(((Number) rowOf(vehicle.getId()).get("version")).longValue())
                    .isGreaterThan(0L);
        }
    }

    @Nested
    @DisplayName("logical removal")
    class LogicalRemoval {

        @Test
        @DisplayName("keeps the row and frees the plate for a new registration")
        void keepsTheRowAndReleasesThePlate() {
            Vehicle first = register("AUD4D44");
            remove.remove(first.getId(), ACTOR);

            // The row survives: the service history attached to it is a legal and warranty
            // obligation (LGPD Art. 16 I), and deleting it would take that with it.
            assertThat(jdbc.queryForObject("SELECT count(*) FROM vehicles WHERE id = ?", Long.class, first.getId()))
                    .isEqualTo(1L);
            assertThat(rowOf(first.getId()).get("deleted_at")).isNotNull();

            // And it answers nothing.
            assertThat(repository.findActiveById(first.getId())).isEmpty();

            // The partial unique index is what makes this possible: uniqueness holds among active
            // vehicles, not across the table.
            Vehicle second = register("AUD4D44");
            assertThat(second.getId()).isNotEqualTo(first.getId());
            assertThat(repository.findActiveByLicensePlate(new LicensePlate("AUD4D44")))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("the field-level trail")
    class Trail {

        @Test
        @DisplayName("records the value before and after, per field")
        void recordsOldAndNewValues() {
            Vehicle vehicle = register("AUD5E55");

            update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, ACTOR));

            List<Map<String, Object>> entries = jdbc.queryForList("""
                    SELECT field_name, old_value, new_value, reason, changed_by
                    FROM audit_trail
                    WHERE aggregate_type = 'Vehicle' AND aggregate_id = ?
                    ORDER BY field_name
                    """, vehicle.getId());

            assertThat(entries).hasSize(3);
            assertThat(entries).extracting("field_name").containsExactly("make", "model", "modelYear");
            assertThat(entries.get(0))
                    .containsEntry("old_value", "Volkswagen")
                    .containsEntry("new_value", "Chevrolet")
                    .containsEntry("changed_by", ACTOR);
            assertThat(entries.get(2)).containsEntry("old_value", "2020").containsEntry("new_value", "2021");

            // Null and not blank: HS9 leaves the semantics of "reason" open, and the use case has
            // nothing to claim here. Recording an empty string would assert that someone gave a
            // reason and left it blank.
            assertThat(entries.get(0).get("reason")).isNull();
        }

        @Test
        @DisplayName("says nothing about a field the request restated without changing")
        void ignoresUnchangedFields() {
            Vehicle vehicle = register("AUD6F66");

            // Only the model year moves. A form posts every field regardless of what the user
            // touched, so the other two arrive unchanged.
            update.update(new UpdateVehicleCommand(vehicle.getId(), "Volkswagen", "Gol", 2021, ACTOR));

            List<String> fields = jdbc.queryForList(
                    "SELECT field_name FROM audit_trail WHERE aggregate_id = ?", String.class, vehicle.getId());

            assertThat(fields).containsExactly("modelYear");
        }

        @Test
        @DisplayName("survives the removal of the vehicle it describes")
        void outlivesTheAggregate() {
            Vehicle vehicle = register("AUD7G77");
            update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, ACTOR));

            remove.remove(vehicle.getId(), ACTOR);

            // No foreign key ties the trail to the vehicle, deliberately: evidence that disappears
            // with the thing it describes is not evidence. This is also the consequence the group
            // accepted on the plate — the old value stays here, retained under LGPD Art. 16 I.
            assertThat(jdbc.queryForObject(
                            "SELECT count(*) FROM audit_trail WHERE aggregate_id = ?", Long.class, vehicle.getId()))
                    .isEqualTo(3L);
        }
    }
}
