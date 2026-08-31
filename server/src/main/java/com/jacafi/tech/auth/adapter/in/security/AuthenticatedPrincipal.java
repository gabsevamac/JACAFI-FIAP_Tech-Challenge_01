package com.jacafi.tech.auth.adapter.in.security;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.domain.entity.Role;

public record AuthenticatedPrincipal(UUID userAccountId, String username, Set<Role> roles, UUID customerId)
        implements Principal {

    public AuthenticatedPrincipal {
        roles = Set.copyOf(roles);
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String toString() {
        return "AuthenticatedPrincipal[userAccountId=%s, roles=%s]".formatted(userAccountId, roles);
    }
}
