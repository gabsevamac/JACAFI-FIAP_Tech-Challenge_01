package com.jacafi.tech.auth.adapter.in.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.exception.AuthenticationFailedException;

@Component
public class SpringSecurityCurrentAuthenticatedUserAdapter implements CurrentAuthenticatedUserPort {

    @Override
    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationFailedException();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedPrincipal authenticated)) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticatedUser(
                authenticated.userAccountId(),
                authenticated.username(),
                authenticated.roles(),
                authenticated.customerId());
    }
}
