package com.jacafi.tech.auth.application.service;

import java.util.UUID;

import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.domain.exception.UserAccountNotFoundException;

public final class DeactivateUserAccountService {

    private final UserAccountRepository accounts;
    private final UserAccountAuthorizationPolicy authorization;

    public DeactivateUserAccountService(UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        this.accounts = accounts;
        this.authorization = authorization;
    }

    public void deactivate(UUID id) {
        authorization.requireManagementOf(id);
        var account = accounts.findById(id).orElseThrow(() -> new UserAccountNotFoundException(id));
        account.deactivate();
        accounts.save(account);
    }
}
