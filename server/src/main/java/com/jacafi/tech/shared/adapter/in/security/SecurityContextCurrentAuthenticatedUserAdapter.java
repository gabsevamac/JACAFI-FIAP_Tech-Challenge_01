package com.jacafi.tech.shared.adapter.in.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.AuthenticationFailedException;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;

public class SecurityContextCurrentAuthenticatedUserAdapter implements CurrentAuthenticatedUserPort {

    @Override
    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticatedUser(
                principal.subject(), principal.username(), principal.roles(), principal.customerId());
    }
}
