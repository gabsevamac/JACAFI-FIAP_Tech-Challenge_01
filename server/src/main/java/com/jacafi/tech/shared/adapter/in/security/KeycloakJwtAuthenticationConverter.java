package com.jacafi.tech.shared.adapter.in.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.shared.security.Role;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String USERNAME_CLAIM = "preferred_username";

    private final CustomerIdentityPort customerIdentities;

    public KeycloakJwtAuthenticationConverter(CustomerIdentityPort customerIdentities) {
        this.customerIdentities = customerIdentities;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<Role> roles = rolesOf(jwt);
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(jwt.getSubject(), usernameOf(jwt), roles, customerIdOf(jwt, roles));
        return new PreAuthenticatedAuthenticationToken(principal, jwt, authoritiesOf(roles));
    }

    private Set<Role> rolesOf(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> claim) || !(claim.get(ROLES_CLAIM) instanceof Collection<?> granted)) {
            return Set.of();
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (Object value : granted) {
            for (Role role : Role.values()) {
                if (role.name().equalsIgnoreCase(String.valueOf(value))) {
                    roles.add(role);
                }
            }
        }
        return Set.copyOf(roles);
    }

    private UUID customerIdOf(Jwt jwt, Set<Role> roles) {
        if (!roles.contains(Role.CUSTOMER)) {
            return null;
        }
        return customerIdentities.customerIdBySubject(jwt.getSubject()).orElse(null);
    }

    private static String usernameOf(Jwt jwt) {
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        return username == null || username.isBlank() ? jwt.getSubject() : username;
    }

    private static List<GrantedAuthority> authoritiesOf(Set<Role> roles) {
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }
}
