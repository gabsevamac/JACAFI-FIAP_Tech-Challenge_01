package com.jacafi.tech.auth.application.service;

import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.UsernameAlreadyExistsException;

public final class CreateUserAccountService {

    private final UserAccountRepositoryPort accounts;
    private final PasswordHashPort passwordHash;
    private final UserAccountAuthorizationPolicy authorization;

    public CreateUserAccountService(
            UserAccountRepositoryPort accounts,
            PasswordHashPort passwordHash,
            UserAccountAuthorizationPolicy authorization) {
        this.accounts = accounts;
        this.passwordHash = passwordHash;
        this.authorization = authorization;
    }

    public UserAccount create(String username, String rawPassword, Set<Role> roles, UUID customerId) {
        authorization.requireAdministrator();
        if (accounts.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }
        UserAccount account =
                UserAccount.create(UUID.randomUUID(), username, passwordHash.hash(rawPassword), roles, customerId);
        return accounts.save(account);
    }
}
