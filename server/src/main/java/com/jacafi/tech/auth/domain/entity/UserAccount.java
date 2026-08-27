package com.jacafi.tech.auth.domain.entity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class UserAccount {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final Set<Role> roles;
    private final UUID customerId;
    private boolean active;

    private UserAccount(
            UUID id, String username, String passwordHash, Set<Role> roles, UUID customerId, boolean active) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
        this.customerId = customerId;
        this.active = active;
        validateRoles();
    }

    public static UserAccount create(UUID id, String username, String passwordHash, Set<Role> roles, UUID customerId) {
        return new UserAccount(id, username, passwordHash, roles, customerId, true);
    }

    public static UserAccount restore(
            UUID id, String username, String passwordHash, Set<Role> roles, UUID customerId, boolean active) {
        return new UserAccount(id, username, passwordHash, roles, customerId, active);
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<Role> roles() {
        return roles;
    }

    public Optional<UUID> customerId() {
        return Optional.ofNullable(customerId);
    }

    public boolean active() {
        return active;
    }

    public boolean canAuthenticate() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    private void validateRoles() {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        if (roles.contains(Role.CUSTOMER) != (customerId != null)) {
            throw new IllegalArgumentException("customerId must exist exactly for customer accounts");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "UserAccount[id=%s, roles=%s, active=%s]".formatted(id, roles, active);
    }
}
