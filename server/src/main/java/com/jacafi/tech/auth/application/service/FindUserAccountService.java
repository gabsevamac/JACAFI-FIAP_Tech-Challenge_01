package com.jacafi.tech.auth.application.service;

import java.util.UUID;

import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.UserAccountNotFoundException;

public final class FindUserAccountService {

    private final UserAccountRepository accounts;
    private final UserAccountAuthorizationPolicy authorization;

    public FindUserAccountService(UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        this.accounts = accounts;
        this.authorization = authorization;
    }

    public UserAccount find(UUID id) {
        authorization.requireManagementOf(id);
        return accounts.findById(id).orElseThrow(() -> new UserAccountNotFoundException(id));
    }
}
