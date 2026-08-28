package com.jacafi.tech.serviceorder.application.service;

import java.util.Set;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;

public final class ServiceOrderAccessPolicy {
    private static final Set<Role> OPERATIONAL_ROLES = Set.of(Role.ADMIN, Role.MANAGER, Role.SERVICE_ADVISOR);
    private final CurrentAuthenticatedUserPort currentUser;

    public ServiceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireOperationalAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(OPERATIONAL_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    String currentActor() {
        return currentUser.currentUser().username();
    }
}
