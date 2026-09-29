package com.jacafi.tech.shared.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(String subject, String username, Set<Role> roles, UUID customerId) {

    public AuthenticatedUser {
        subject = requireText(subject, "subject");
        username = requireText(username, "username");
        roles = Set.copyOf(roles);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[subject=%s, roles=%s]".formatted(subject, roles);
    }
}
