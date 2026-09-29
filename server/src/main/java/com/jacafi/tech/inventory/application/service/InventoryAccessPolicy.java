package com.jacafi.tech.inventory.application.service;

import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

public final class InventoryAccessPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public InventoryAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
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
