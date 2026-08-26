package com.jacafi.tech;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.support.AbstractIntegrationTest;

/**
 * Smoke test: the application context starts against a real Postgres, which also means every
 * Flyway migration applied and every JPA mapping validated against the resulting schema.
 */
class TechApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {}
}
