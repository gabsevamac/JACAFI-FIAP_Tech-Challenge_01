package com.jacafi.tech.servicecatalog.application.service;

import java.util.Set;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;

public final class ServiceCatalogAccessPolicy {
    private static final Set<Role> OPERATIONAL_ROLES = Set.of(Role.ADMIN, Role.MANAGER, Role.SERVICE_ADVISOR);
    private static final Set<Role> MANAGEMENT_ROLES = Set.of(Role.ADMIN, Role.MANAGER);

    private final CurrentAuthenticatedUserPort currentUser;

    public ServiceCatalogAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireOperationalAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(OPERATIONAL_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    void requireManagementAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(MANAGEMENT_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    String currentActor() {
        return currentUser.currentUser().username();
    }
}
