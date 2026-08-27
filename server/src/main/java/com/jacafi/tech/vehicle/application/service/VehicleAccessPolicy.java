package com.jacafi.tech.vehicle.application.service;

import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;

public final class VehicleAccessPolicy {

    private static final Set<Role> OPERATIONAL_ROLES = Set.of(Role.ADMIN, Role.MANAGER, Role.SERVICE_ADVISOR);

    private final CurrentAuthenticatedUserPort currentUser;

    public VehicleAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireOperationalAccess() {
        if (currentUser.currentUser().roles().stream().noneMatch(OPERATIONAL_ROLES::contains)) {
            throw new AccountAccessDeniedException();
        }
    }

    UUID currentCustomerId() {
        AuthenticatedUser user = currentUser.currentUser();
        if (!user.roles().contains(Role.CUSTOMER) || user.customerId() == null) {
            throw new AccountAccessDeniedException();
        }
        return user.customerId();
    }

    String currentActor() {
        return currentUser.currentUser().username();
    }
}
