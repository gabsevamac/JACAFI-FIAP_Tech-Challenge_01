package com.jacafi.tech.laboroperation.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.auth.adapter.in.security.JwtAuthenticationFilter;
import com.jacafi.tech.auth.adapter.in.security.SpringSecurityCurrentAuthenticatedUserAdapter;
import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.laboroperation.adapter.in.web.controller.LaborOperationController;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.config.LaborOperationConfiguration;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.config.TimeConfiguration;

@WebMvcTest(LaborOperationController.class)
@Import({
    LaborOperationConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    JwtAuthenticationFilter.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    SpringSecurityCurrentAuthenticatedUserAdapter.class
})
class LaborOperationSecurityMvcTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccessTokenPort accessTokens;

    @MockitoBean
    private UserAccountRepository accounts;

    @MockitoBean
    private LaborOperationRepository operations;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @BeforeEach
    void setUp() {
        when(accessTokens.parseSubject("customer-token")).thenReturn("customer");
        when(accounts.findByUsername("customer"))
                .thenReturn(Optional.of(UserAccount.restore(
                        UUID.randomUUID(), "customer", "hash", Set.of(Role.CUSTOMER), UUID.randomUUID(), true)));
    }

    @Test
    void customerCannotAccessLaborOperations() throws Exception {
        mvc.perform(get("/api/v1/labor-operations").header("Authorization", "Bearer customer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC-002"));

        verifyNoInteractions(operations, auditTrail);
    }
}
