package com.jacafi.tech.auth.application.service;

import java.util.List;

import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.domain.entity.UserAccount;

public final class ListUserAccountsService {

    private final UserAccountRepository accounts;
    private final UserAccountAuthorizationPolicy authorization;

    public ListUserAccountsService(UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        this.accounts = accounts;
        this.authorization = authorization;
    }

    public List<UserAccount> list() {
        var administrator = authorization.requireAdministrator();
        return accounts.findAll().stream()
                .filter(account -> !account.id().equals(administrator.userAccountId()))
                .toList();
    }
}
