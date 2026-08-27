package com.jacafi.tech.auth.application.port;

import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.domain.entity.Role;

public record AuthenticatedUser(UUID userAccountId, Set<Role> roles, UUID customerId) {
    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }
}
