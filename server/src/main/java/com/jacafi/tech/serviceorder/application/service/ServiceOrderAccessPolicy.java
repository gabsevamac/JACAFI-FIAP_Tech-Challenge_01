package com.jacafi.tech.serviceorder.application.service;

import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;

public final class ServiceOrderAccessPolicy {
    private static final Set<Role> OPERATIONAL_ROLES = Set.of(Role.ADMIN, Role.MANAGER, Role.SERVICE_ADVISOR);
    private static final Set<Role> STATUS_MANAGEMENT_ROLES =
            Set.of(Role.ADMIN, Role.MANAGER, Role.SERVICE_ADVISOR, Role.TECHNICIAN);
    private final CurrentAuthenticatedUserPort currentUser;

    public ServiceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireOperationalAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(OPERATIONAL_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    void requireStatusManagementAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(STATUS_MANAGEMENT_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    void requireReadAccess(UUID customerId) {
        AuthenticatedUser user = currentUser.currentUser();
        if (user.roles().stream().anyMatch(OPERATIONAL_ROLES::contains)) {
            return;
        }
        if (!user.roles().contains(Role.CUSTOMER) || !customerId.equals(user.customerId())) {
            throw new AccountAccessDeniedException();
        }
    }

    String currentActor() {
        return currentUser.currentUser().username();
    }
}
