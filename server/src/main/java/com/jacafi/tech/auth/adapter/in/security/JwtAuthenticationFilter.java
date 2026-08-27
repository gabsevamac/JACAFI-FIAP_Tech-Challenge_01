package com.jacafi.tech.auth.adapter.in.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.InvalidAccessTokenException;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.UserAccount;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenPort accessTokens;
    private final UserAccountRepositoryPort accounts;

    public JwtAuthenticationFilter(AccessTokenPort accessTokens, UserAccountRepositoryPort accounts) {
        this.accessTokens = accessTokens;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            String username = accessTokens.parseSubject(token);
            accounts.findByUsername(username)
                    .filter(UserAccount::canAuthenticate)
                    .ifPresent(account -> {
                        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                                account.id(),
                                account.username(),
                                account.roles(),
                                account.customerId().orElse(null));
                        var authorities = account.roles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList();
                        SecurityContextHolder.getContext()
                                .setAuthentication(
                                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
                    });
        } catch (InvalidAccessTokenException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
