package com.jacafi.tech.shared.adapter.in.security;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.shared.security.Role;

public record AuthenticatedPrincipal(String subject, String username, Set<Role> roles, UUID customerId)
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
        return "AuthenticatedPrincipal[subject=%s, roles=%s]".formatted(subject, roles);
    }
}
