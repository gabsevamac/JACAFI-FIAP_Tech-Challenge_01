package com.jacafi.tech.auth.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.jacafi.tech.auth.domain.exception.AuthenticationFailedException;

class SpringSecurityCurrentAuthenticatedUserAdapterTest {

    private final SpringSecurityCurrentAuthenticatedUserAdapter adapter =
            new SpringSecurityCurrentAuthenticatedUserAdapter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRequestsWithoutAuthentication() {
        assertThatThrownBy(adapter::currentUser).isInstanceOf(AuthenticationFailedException.class);
    }
}
