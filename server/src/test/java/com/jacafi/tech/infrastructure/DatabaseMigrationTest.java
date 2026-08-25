package com.jacafi.tech.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///jacafi",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=jacafi",
        "spring.datasource.password=jacafi"
})
class DatabaseMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void appliesTheInitialSchemaAndClientIndexes() {
        var tables = jdbcClient.sql("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        """)
                .query(String.class)
                .list();

        var indexes = jdbcClient.sql("""
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                        """)
                .query(String.class)
                .list();

        assertThat(tables).contains(
                "flyway_schema_history",
                "parties",
                "clients",
                "services",
                "service_orders",
                "service_orders_service");
        assertThat(indexes).contains("uk_parties_tax_identifier");
    }
}
