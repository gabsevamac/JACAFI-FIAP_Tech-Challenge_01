package com.jacafi.tech.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces the Keycloak JWKS decoder so tests do not need a running identity provider. Everything
 * downstream — the role converter, the filter chain and the access policies — runs unchanged.
 */
@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(TestTokens.KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
