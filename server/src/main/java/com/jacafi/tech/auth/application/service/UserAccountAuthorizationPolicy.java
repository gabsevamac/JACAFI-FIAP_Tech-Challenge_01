package com.jacafi.tech.auth.application.service;

import java.util.UUID;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;

public final class UserAccountAuthorizationPolicy {

    private final CurrentAuthenticatedUserPort currentUser;

    public UserAccountAuthorizationPolicy(CurrentAuthenticatedUserPort currentUser) {
        this.currentUser = currentUser;
    }

    public AuthenticatedUser requireAdministrator() {
        AuthenticatedUser user = currentUser.currentUser();
        if (!user.roles().contains(Role.ADMIN)) {
            throw new AccountAccessDeniedException();
        }
        return user;
    }

    public void requireManagementOf(UUID userAccountId) {
        if (requireAdministrator().userAccountId().equals(userAccountId)) {
            throw new AccountAccessDeniedException();
        }
    }
}
