package com.jacafi.tech.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class DatabaseMigrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeAll
    static void migrateDatabase() {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Test
    void appliesTheVersionedSchema() throws SQLException {
        assertThat(strings("""
                        SELECT script
                        FROM flyway_schema_history
                        WHERE success
                        ORDER BY installed_rank
                        """))
                .containsExactly(
                        "V01_20260827__create_user_accounts.sql",
                        "V02_20260827__create_customers.sql",
                        "V03_20260827__create_vehicles.sql",
                        "V04_20260827__create_inventory.sql",
                        "V05_20260827__create_service_catalog.sql",
                        "V06_20260827__create_service_orders.sql",
                        "V07_20260827__create_audit_trail.sql",
                        "V08_20260827__seed_admin_account.sql",
                        "V09_20260830__create_event_outbox.sql");

        assertThat(strings("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        """))
                .contains(
                        "user_accounts",
                        "user_account_roles",
                        "customers",
                        "vehicles",
                        "inventory_items",
                        "inventory_reservations",
                        "inventory_audit_entries",
                        "service_catalog_items",
                        "service_orders",
                        "service_order_service_lines",
                        "service_order_material_lines",
                        "service_order_estimates",
                        "service_order_estimate_decisions",
                        "service_order_status_history",
                        "audit_trail",
                        "event_outbox")
                .doesNotContain("users", "clients", "parties", "services", "service_orders_service");
    }

    @Test
    void createsTheRequiredForeignKeysAndIndexes() throws SQLException {
        assertThat(strings("""
                        SELECT conname
                        FROM pg_constraint
                        WHERE contype = 'f'
                        """))
                .contains(
                        "fk_user_accounts_customer",
                        "fk_vehicles_customer",
                        "fk_inventory_reservations_item",
                        "fk_inventory_reservations_service_order",
                        "fk_inventory_audit_entries_item",
                        "fk_service_orders_customer",
                        "fk_service_orders_vehicle",
                        "fk_service_order_service_lines_order",
                        "fk_service_order_service_lines_catalog_item",
                        "fk_service_order_material_lines_order",
                        "fk_service_order_material_lines_inventory_item",
                        "fk_service_order_estimates_order",
                        "fk_service_order_estimate_decisions_order",
                        "fk_service_order_estimate_decisions_estimate",
                        "fk_service_order_status_history_order",
                        "fk_user_account_roles_account");

        var indexes = strings("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                """);
        assertThat(indexes)
                .contains(
                        "uk_user_accounts_username",
                        "uk_user_accounts_customer",
                        "uk_customers_tax_id",
                        "uk_vehicles_active_license_plate",
                        "ix_vehicles_customer_id",
                        "uk_inventory_items_active_name",
                        "uk_inventory_reservations_item_order",
                        "uk_service_catalog_items_active_name",
                        "uk_service_order_estimate_decisions_idempotency_key",
                        "ix_service_orders_operational_queue");

        var operationalIndex = string("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'ix_service_orders_operational_queue'
                """);
        assertThat(operationalIndex)
                .contains("CASE status")
                .contains("IN_PROGRESS")
                .contains("AWAITING_APPROVAL")
                .contains("UNDER_DIAGNOSIS")
                .contains("RECEIVED")
                .contains("created_at", "id")
                .doesNotContain("priority");
        assertThat(strings("""
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'service_orders'
                        """)).doesNotContain("priority");
    }

    @Test
    void allowsReReservationAfterEarlierReservationIsDeleted() throws SQLException {
        execute("""
                INSERT INTO customers (
                    id, tax_id, name, email, phone, created_at, created_by, updated_at, updated_by
                ) VALUES (
                    '10000000-0000-0000-0000-000000000001', '12345678901', 'Test customer',
                    'test@example.com', '11999999999', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                );
                INSERT INTO vehicles (
                    id, license_plate, make, model, model_year, customer_id,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    '20000000-0000-0000-0000-000000000001', 'TST1A23', 'Test', 'Test', 2026,
                    '10000000-0000-0000-0000-000000000001',
                    CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                );
                INSERT INTO service_orders (
                    id, customer_id, vehicle_id, status, reported_issue,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    '30000000-0000-0000-0000-000000000001',
                    '10000000-0000-0000-0000-000000000001',
                    '20000000-0000-0000-0000-000000000001',
                    'RECEIVED', 'Test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                );
                INSERT INTO inventory_items (
                    id, name, type, unit_price, stock_on_hand,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    '40000000-0000-0000-0000-000000000001', 'Test item', 'PART', 1.00, 2,
                    CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                );
                INSERT INTO inventory_reservations (
                    id, inventory_item_id, service_order_id, quantity, reserved_at,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    '50000000-0000-0000-0000-000000000001',
                    '40000000-0000-0000-0000-000000000001',
                    '30000000-0000-0000-0000-000000000001',
                    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                );
                UPDATE inventory_reservations
                SET deleted_at = CURRENT_TIMESTAMP,
                    deleted_by = 'test',
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = 'test',
                    version = version + 1
                WHERE id = '50000000-0000-0000-0000-000000000001';
                """);

        assertThatCode(() -> execute("""
                        INSERT INTO inventory_reservations (
                            id, inventory_item_id, service_order_id, quantity, reserved_at,
                            created_at, created_by, updated_at, updated_by
                        ) VALUES (
                            '50000000-0000-0000-0000-000000000002',
                            '40000000-0000-0000-0000-000000000001',
                            '30000000-0000-0000-0000-000000000001',
                            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test'
                        );
                        """)).doesNotThrowAnyException();

        assertThat(string("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uk_inventory_reservations_item_order'
                """)).contains("WHERE (deleted_at IS NULL)");
    }

    @Test
    void enforcesTargetValuesAndDataShape() throws SQLException {
        var checks = strings("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE contype = 'c'
                """);
        assertThat(checks)
                .anyMatch(definition -> definition.contains("ADMIN")
                        && definition.contains("MANAGER")
                        && definition.contains("SERVICE_ADVISOR")
                        && definition.contains("TECHNICIAN")
                        && definition.contains("CUSTOMER"))
                .anyMatch(definition -> definition.contains("RECEIVED")
                        && definition.contains("UNDER_DIAGNOSIS")
                        && definition.contains("AWAITING_APPROVAL")
                        && definition.contains("IN_PROGRESS")
                        && definition.contains("COMPLETED")
                        && definition.contains("DELIVERED"))
                .anyMatch(definition -> definition.contains("PENDING")
                        && definition.contains("APPROVED")
                        && definition.contains("REJECTED"))
                .anyMatch(definition -> definition.contains("APPROVE") && definition.contains("REJECT"))
                .anyMatch(definition -> definition.contains("tax_id")
                        && definition.contains("[0-9]{11}")
                        && definition.contains("[A-Z0-9]{12}[0-9]{2}"));
        assertThat(strings("""
                        SELECT match[1]
                        FROM (
                            SELECT regexp_matches(
                                pg_get_constraintdef(oid),
                                '''([A-Z_]+)''',
                                'g'
                            ) AS match
                            FROM pg_constraint
                            WHERE conname = 'ck_service_orders_status'
                        ) approved_statuses
                        ORDER BY match[1]
                        """))
                .containsExactly(
                        "AWAITING_APPROVAL", "COMPLETED", "DELIVERED", "IN_PROGRESS", "RECEIVED", "UNDER_DIAGNOSIS");
        assertThat(strings("""
                        SELECT conname
                        FROM pg_constraint
                        WHERE contype = 'c'
                        """))
                .contains(
                        "ck_inventory_items_unit_price",
                        "ck_inventory_items_stock_on_hand",
                        "ck_inventory_reservations_quantity",
                        "ck_service_catalog_items_base_price",
                        "ck_service_order_service_lines_price",
                        "ck_service_order_service_lines_quantity",
                        "ck_service_order_material_lines_price",
                        "ck_service_order_material_lines_quantity",
                        "ck_service_order_estimates_total_amount");
        assertThat(strings("""
                        SELECT trigger_name
                        FROM information_schema.triggers
                        WHERE trigger_schema = 'public'
                        """)).contains("trg_inventory_audit_entries_append_only", "trg_audit_trail_append_only");

        assertThat(strings("""
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'audit_trail'
                        """))
                .contains(
                        "outbox_event_id",
                        "aggregate_type",
                        "aggregate_id",
                        "action",
                        "actor",
                        "occurred_at",
                        "before_state",
                        "after_state")
                .doesNotContain(
                        "metadata", "old_value", "new_value", "cpf", "cnpj", "tax_id", "plate", "password", "token");

        assertThat(strings("""
                        SELECT username || ':' || active || ':' || role
                        FROM user_accounts
                        JOIN user_account_roles ON user_account_id = user_accounts.id
                        """)).containsExactly("dev-admin:true:ADMIN");
        assertThat(string("""
                SELECT password_hash
                FROM user_accounts
                WHERE username = 'dev-admin'
                """)).matches("^\\$2[aby]\\$[0-9]{2}\\$.{53}$");
    }

    @Test
    void usesTimeZoneAwareTimestampsAndBusinessAuditColumns() throws SQLException {
        assertThat(strings("""
                        SELECT table_name || '.' || column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND data_type = 'timestamp without time zone'
                          AND table_name <> 'flyway_schema_history'
                        """)).isEmpty();

        for (String table : List.of(
                "user_accounts",
                "customers",
                "vehicles",
                "inventory_items",
                "inventory_reservations",
                "service_catalog_items",
                "service_orders",
                "service_order_service_lines",
                "service_order_material_lines",
                "service_order_estimates")) {
            assertThat(strings("""
                            SELECT column_name
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = '%s'
                            """.formatted(table)))
                    .as(table)
                    .contains(
                            "created_at",
                            "created_by",
                            "updated_at",
                            "updated_by",
                            "deleted_at",
                            "deleted_by",
                            "version");
        }
    }

    private static List<String> strings(String sql) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            var values = new java.util.ArrayList<String>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return values;
        }
    }

    private static String string(String sql) throws SQLException {
        return strings(sql).getFirst();
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connection();
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
