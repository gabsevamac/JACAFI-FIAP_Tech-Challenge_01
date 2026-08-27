package com.jacafi.tech.auth.application.service;

import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.AuthenticationFailedException;

public final class AuthenticationService {

    private final UserAccountRepositoryPort accounts;
    private final PasswordHashPort passwordHash;
    private final AccessTokenPort accessTokens;

    public AuthenticationService(
            UserAccountRepositoryPort accounts, PasswordHashPort passwordHash, AccessTokenPort accessTokens) {
        this.accounts = accounts;
        this.passwordHash = passwordHash;
        this.accessTokens = accessTokens;
    }

    public LoginResult login(String username, String rawPassword) {
        UserAccount account = accounts.findByUsername(username).orElseThrow(AuthenticationFailedException::new);
        if (!account.canAuthenticate() || !passwordHash.matches(rawPassword, account.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        return new LoginResult(account.username(), accessTokens.issue(account.username()));
    }
}
