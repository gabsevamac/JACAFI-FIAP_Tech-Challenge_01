package com.jacafi.tech.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Mints access tokens shaped like the ones the Keycloak realm issues, signed with a symmetric key
 * that only the test {@link TestSecurityConfiguration} decoder trusts.
 */
public final class TestTokens {

    static final String SECRET = "test-only-secret-key-with-at-least-32-bytes-of-length";
    static final SecretKey KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private TestTokens() {}

    public static String employeeBearer(String username) {
        return "Bearer " + employee(username);
    }

    public static String employee(String username) {
        return token(UUID.randomUUID().toString(), username, "employee");
    }

    public static String customer(String subject, String username) {
        return token(subject, username, "customer");
    }

    public static String token(String subject, String username, String... realmRoles) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("https://keycloak.test/realms/jacafi")
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", List.of(realmRoles)))
                .issueTime(java.util.Date.from(Instant.now()))
                .expirationTime(java.util.Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(KEY));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not mint a test token", exception);
        }
    }
}
