package com.jacafi.tech.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.application.service.AuthenticationService;
import com.jacafi.tech.auth.application.service.UserAccountService;

@Configuration
public class AuthConfiguration {

    @Bean
    AuthenticationService authenticationService(
            UserAccountRepositoryPort accounts, PasswordHashPort passwordHash, AccessTokenPort accessTokens) {
        return new AuthenticationService(accounts, passwordHash, accessTokens);
    }

    @Bean
    UserAccountService userAccountService(
            UserAccountRepositoryPort accounts,
            PasswordHashPort passwordHash,
            CurrentAuthenticatedUserPort currentUser) {
        return new UserAccountService(accounts, passwordHash, currentUser);
    }
}
