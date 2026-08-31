package com.jacafi.tech.auth.application.port;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.domain.entity.Role;

public record AuthenticatedUser(UUID userAccountId, String username, Set<Role> roles, UUID customerId) {
    public AuthenticatedUser {
        Objects.requireNonNull(userAccountId, "userAccountId must not be null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        roles = Set.copyOf(roles);
    }

    public AuthenticatedUser(UUID userAccountId, Set<Role> roles, UUID customerId) {
        this(userAccountId, userAccountId.toString(), roles, customerId);
    }
}
