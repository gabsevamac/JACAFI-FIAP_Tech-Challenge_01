package com.jacafi.tech.vehicle.application.service;

import java.util.UUID;

import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

public final class VehicleAccessPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public VehicleAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireEmployee() {
        if (!currentUser.currentUser().roles().contains(Role.EMPLOYEE)) {
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
