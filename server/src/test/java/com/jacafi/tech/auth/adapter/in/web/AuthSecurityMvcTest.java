package com.jacafi.tech.auth.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import com.jacafi.tech.auth.adapter.in.web.controller.AuthController;
import com.jacafi.tech.auth.adapter.in.web.controller.UserAccountController;
import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.application.service.AuthenticateUserService;
import com.jacafi.tech.auth.application.service.CreateUserAccountService;
import com.jacafi.tech.auth.application.service.DeactivateUserAccountService;
import com.jacafi.tech.auth.application.service.FindUserAccountService;
import com.jacafi.tech.auth.application.service.GetCurrentUserAccountService;
import com.jacafi.tech.auth.application.service.ListUserAccountsService;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;

@WebMvcTest({AuthController.class, UserAccountController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityProblemDetailHandler.class})
class AuthSecurityMvcTest {

    private static final UserAccount ADMIN = account("10000000-0000-0000-0000-000000000001", "admin", Role.ADMIN);
    private static final UserAccount TECHNICIAN =
            account("10000000-0000-0000-0000-000000000002", "technician", Role.TECHNICIAN);
    private static final UserAccount CUSTOMER = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000003"),
            "customer",
            "hash",
            Set.of(Role.CUSTOMER),
            UUID.fromString("20000000-0000-0000-0000-000000000003"),
            true);
    private static final UserAccount CUSTOMER_SERVICE_ADVISOR = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000005"),
            "customer-service-advisor",
            "hash",
            Set.of(Role.CUSTOMER, Role.SERVICE_ADVISOR),
            UUID.fromString("20000000-0000-0000-0000-000000000005"),
            true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthenticateUserService authenticateUserService;

    @MockitoBean
    private CreateUserAccountService createUserAccountService;

    @MockitoBean
    private ListUserAccountsService listUserAccountsService;

    @MockitoBean
    private FindUserAccountService findUserAccountService;

    @MockitoBean
    private GetCurrentUserAccountService getCurrentUserAccountService;

    @MockitoBean
    private DeactivateUserAccountService deactivateUserAccountService;

    @MockitoBean
    private AccessTokenPort accessTokens;

    @MockitoBean
    private UserAccountRepositoryPort accounts;

    @BeforeEach
    void setUp() {
        when(accessTokens.parseSubject("admin-token")).thenReturn("admin");
        when(accessTokens.parseSubject("technician-token")).thenReturn("technician");
        when(accessTokens.parseSubject("customer-token")).thenReturn("customer");
        when(accessTokens.parseSubject("customer-service-advisor-token")).thenReturn("customer-service-advisor");
        when(accessTokens.parseSubject("inactive-token")).thenReturn("inactive");
        when(accounts.findByUsername("admin")).thenReturn(java.util.Optional.of(ADMIN));
        when(accounts.findByUsername("technician")).thenReturn(java.util.Optional.of(TECHNICIAN));
        when(accounts.findByUsername("customer")).thenReturn(java.util.Optional.of(CUSTOMER));
        when(accounts.findByUsername("customer-service-advisor"))
                .thenReturn(java.util.Optional.of(CUSTOMER_SERVICE_ADVISOR));
        when(accounts.findByUsername("inactive"))
                .thenReturn(java.util.Optional.of(UserAccount.restore(
                        UUID.fromString("10000000-0000-0000-0000-000000000004"),
                        "inactive",
                        "hash",
                        Set.of(Role.ADMIN),
                        null,
                        false)));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/v1/user-accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SEG-001"));
    }

    @Test
    void deniesManagementToNonAdmin() throws Exception {
        mvc.perform(get("/api/v1/user-accounts").header("Authorization", "Bearer technician-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));
    }

    @Test
    void allowsAdminManagement() throws Exception {
        when(listUserAccountsService.list()).thenReturn(List.of(ADMIN));

        mvc.perform(get("/api/v1/user-accounts").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADMIN.id().toString()))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void allowsCustomerToReadOnlyCurrentAccount() throws Exception {
        when(getCurrentUserAccountService.get()).thenReturn(CUSTOMER);

        mvc.perform(get("/api/v1/user-accounts/me").header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER.id().toString()))
                .andExpect(jsonPath("$.customerId")
                        .value(CUSTOMER.customerId().orElseThrow().toString()));
    }

    @Test
    void allowsMultiRoleCustomerToReadOnlyCurrentAccount() throws Exception {
        when(getCurrentUserAccountService.get()).thenReturn(CUSTOMER_SERVICE_ADVISOR);

        mvc.perform(get("/api/v1/user-accounts/me").header("Authorization", "Bearer customer-service-advisor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_SERVICE_ADVISOR.id().toString()))
                .andExpect(jsonPath("$.customerId")
                        .value(CUSTOMER_SERVICE_ADVISOR
                                .customerId()
                                .orElseThrow()
                                .toString()));

        mvc.perform(get("/api/v1/user-accounts").header("Authorization", "Bearer customer-service-advisor-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));
    }

    @Test
    void rejectsAnOldTokenAfterAccountDeactivation() throws Exception {
        mvc.perform(get("/api/v1/user-accounts/me").header("Authorization", "Bearer inactive-token"))
                .andExpect(status().isUnauthorized());
    }

    private static UserAccount account(String id, String username, Role role) {
        return UserAccount.restore(UUID.fromString(id), username, "hash", Set.of(role), null, true);
    }
}
