package com.jacafi.tech.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need the full application context and a real database.
 *
 * <p>Postgres, not H2: an in-memory database does not enforce the partial unique index that
 * guards license plate uniqueness among active vehicles, so a green test on H2 would prove
 * nothing about the constraint that matters.
 *
 * <p>The container is {@code static}, so a single Postgres instance is shared by every test
 * class that extends this one. Flyway builds the schema, and {@code ddl-auto: validate} then
 * checks the mapped entities against it.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
}
