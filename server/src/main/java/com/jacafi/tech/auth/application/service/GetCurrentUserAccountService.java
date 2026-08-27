package com.jacafi.tech.auth.application.service;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.UserAccountNotFoundException;

public final class GetCurrentUserAccountService {

    private final UserAccountRepositoryPort accounts;
    private final CurrentAuthenticatedUserPort currentUser;

    public GetCurrentUserAccountService(UserAccountRepositoryPort accounts, CurrentAuthenticatedUserPort currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    public UserAccount get() {
        var userAccountId = currentUser.currentUser().userAccountId();
        return accounts.findById(userAccountId).orElseThrow(() -> new UserAccountNotFoundException(userAccountId));
    }
}
