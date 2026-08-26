package com.jacafi.tech.shared.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Answers "who is writing this row", reading the authenticated subject from the security context.
 *
 * <p>The subject of the JWT, which for this application is the username the token was minted for.
 */
public class JwtAuditorAware implements AuditorAware<String> {

    /**
     * Author recorded when nothing is authenticated: a Flyway migration, a scheduled job, a seed.
     *
     * <p>Never null and never blank. Returning an empty {@code Optional} would leave
     * {@code created_by} null, and a nullable author column means every query that reports on
     * authorship has to decide what null means — usually by omitting the row, which is how an
     * unattributed write becomes an invisible one. "system" is a claim that can be checked; null
     * is an absence that cannot.
     */
    public static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // isAuthenticated() alone is not enough: the anonymous authentication filter installs an
        // AnonymousAuthenticationToken that answers true, with "anonymousUser" as its name. Left
        // unchecked, unauthenticated writes would be attributed to a user by that name.
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM);
        }

        String name = authentication.getName();
        return Optional.of(name == null || name.isBlank() ? SYSTEM : name);
    }
}
