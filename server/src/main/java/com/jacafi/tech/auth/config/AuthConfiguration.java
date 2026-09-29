package com.jacafi.tech.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.application.service.AuthenticateUserService;
import com.jacafi.tech.auth.application.service.CreateUserAccountService;
import com.jacafi.tech.auth.application.service.DeactivateUserAccountService;
import com.jacafi.tech.auth.application.service.FindUserAccountService;
import com.jacafi.tech.auth.application.service.GetCurrentUserAccountService;
import com.jacafi.tech.auth.application.service.ListUserAccountsService;
import com.jacafi.tech.auth.application.service.UserAccountAuthorizationPolicy;

@Configuration
public class AuthConfiguration {

    @Bean
    AuthenticateUserService authenticateUserService(
            UserAccountRepository accounts, PasswordHashPort passwordHash, AccessTokenPort accessTokens) {
        return new AuthenticateUserService(accounts, passwordHash, accessTokens);
    }

    @Bean
    UserAccountAuthorizationPolicy userAccountAuthorizationPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new UserAccountAuthorizationPolicy(currentUser);
    }

    @Bean
    CreateUserAccountService createUserAccountService(
            UserAccountRepository accounts,
            PasswordHashPort passwordHash,
            UserAccountAuthorizationPolicy authorization) {
        return new CreateUserAccountService(accounts, passwordHash, authorization);
    }

    @Bean
    ListUserAccountsService listUserAccountsService(
            UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        return new ListUserAccountsService(accounts, authorization);
    }

    @Bean
    FindUserAccountService findUserAccountService(
            UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        return new FindUserAccountService(accounts, authorization);
    }

    @Bean
    GetCurrentUserAccountService getCurrentUserAccountService(
            UserAccountRepository accounts, CurrentAuthenticatedUserPort currentUser) {
        return new GetCurrentUserAccountService(accounts, currentUser);
    }

    @Bean
    DeactivateUserAccountService deactivateUserAccountService(
            UserAccountRepository accounts, UserAccountAuthorizationPolicy authorization) {
        return new DeactivateUserAccountService(accounts, authorization);
    }
}
