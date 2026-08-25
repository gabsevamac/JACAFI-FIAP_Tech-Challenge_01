package com.jacafi.tech;

import com.jacafi.tech.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: the application context starts against a real Postgres, which also means every
 * Flyway migration applied and every JPA mapping validated against the resulting schema.
 */
class TechApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
