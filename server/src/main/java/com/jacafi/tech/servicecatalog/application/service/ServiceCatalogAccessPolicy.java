package com.jacafi.tech.servicecatalog.application.service;

import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

public final class ServiceCatalogAccessPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public ServiceCatalogAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
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
