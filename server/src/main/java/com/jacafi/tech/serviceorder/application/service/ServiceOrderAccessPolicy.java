package com.jacafi.tech.serviceorder.application.service;

import java.util.UUID;

import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

public final class ServiceOrderAccessPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public ServiceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireEmployee() {
        if (!currentUser.currentUser().roles().contains(Role.EMPLOYEE)) {
            throw new AccountAccessDeniedException();
        }
    }

    void requireReadAccess(UUID customerId) {
        AuthenticatedUser user = currentUser.currentUser();
        if (user.roles().contains(Role.EMPLOYEE)) {
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
