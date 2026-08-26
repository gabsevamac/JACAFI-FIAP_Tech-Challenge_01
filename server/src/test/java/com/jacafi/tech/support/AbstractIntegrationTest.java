package com.jacafi.tech.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need the full application context and a real database.
 *
 * <p>Postgres, not H2: an in-memory database does not enforce the partial unique indexes that
 * guard license plate uniqueness among active vehicles and material name uniqueness among active
 * stock items, so a green test on H2 would prove nothing about the constraints that matter.
 *
 * <h2>Why the container is started by hand</h2>
 *
 * <p>This class used to carry {@code @Testcontainers} with {@code @Container} on the field, which
 * is the documented way to let the JUnit extension manage a container's lifecycle. It does not
 * survive a shared base class: the extension stops the container when the <em>first</em> test
 * class finishes, and restarts it — on a new random port — for the next one. Spring caches
 * application contexts by configuration, so every later class that matches an already-cached
 * context keeps the connection pool built for the port that is now gone, and every test in it
 * fails with {@code Connection is not available, request timed out} against a pool holding zero
 * connections.
 *
 * <p>The failure stayed hidden while there was exactly one test class per context configuration.
 * It appears the moment a second slice adds its own integration tests, which is what happened.
 *
 * <p>Started once in a static initializer instead, the container lives for the whole JVM and its
 * port never moves. Nothing stops it explicitly: Testcontainers' Ryuk sidecar removes it when the
 * JVM exits, which is the point of the singleton container pattern.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
