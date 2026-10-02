package com.jacafi.tech.laboroperation.application.service;

import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

public final class LaborOperationAccessPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public LaborOperationAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    void requireEmployee() {
        if (!currentUser.currentUser().roles().contains(Role.EMPLOYEE)) {
            throw new AccountAccessDeniedException();
        }
    }

    String currentActor() {
        return currentUser.currentUser().username();
    }
}
