package com.jacafi.tech.customer.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.auth.adapter.in.security.JwtAuthenticationFilter;
import com.jacafi.tech.auth.adapter.in.security.SpringSecurityCurrentAuthenticatedUserAdapter;
import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.customer.adapter.in.web.controller.CustomerController;
import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.config.CustomerConfiguration;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;

@WebMvcTest(CustomerController.class)
@Import({
    CustomerConfiguration.class,
    GlobalExceptionHandler.class,
    JwtAuthenticationFilter.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    SpringSecurityCurrentAuthenticatedUserAdapter.class
})
class CustomerSecurityMvcTest {

    private static final UUID CUSTOMER_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UserAccount CUSTOMER_A = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "customer-a",
            "hash",
            Set.of(Role.CUSTOMER),
            CUSTOMER_A_ID,
            true);
    private static final UserAccount SERVICE_ADVISOR = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000003"),
            "advisor",
            "hash",
            Set.of(Role.SERVICE_ADVISOR),
            null,
            true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccessTokenPort accessTokens;

    @MockitoBean
    private UserAccountRepositoryPort accounts;

    @MockitoBean
    private CustomerRepositoryPort customers;

    @BeforeEach
    void setUp() {
        when(accessTokens.parseSubject("customer-a-token")).thenReturn("customer-a");
        when(accessTokens.parseSubject("advisor-token")).thenReturn("advisor");
        when(accounts.findByUsername("customer-a")).thenReturn(Optional.of(CUSTOMER_A));
        when(accounts.findByUsername("advisor")).thenReturn(Optional.of(SERVICE_ADVISOR));
    }

    @Test
    void customerCannotReadOrUpdateAnotherCustomerById() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", CUSTOMER_B_ID).header("Authorization", "Bearer customer-a-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        mvc.perform(patch("/api/v1/customers/{id}", CUSTOMER_B_ID)
                        .header("Authorization", "Bearer customer-a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Customer B","email":"b@example.com","phone":"11999999999"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        verifyNoInteractions(customers);
    }

    @Test
    void customerCanReadAndUpdateOnlyTheCurrentCustomer() throws Exception {
        Customer customer = customer(CUSTOMER_A_ID);
        when(customers.findById(CUSTOMER_A_ID)).thenReturn(Optional.of(customer));
        when(customers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(get("/api/v1/customers/me").header("Authorization", "Bearer customer-a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_A_ID.toString()));

        mvc.perform(patch("/api/v1/customers/me")
                        .header("Authorization", "Bearer customer-a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria da Silva","email":"nova@example.com","phone":"11888888888"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"));
    }

    @Test
    void serviceAdvisorCanUseOperationalCustomerLookup() throws Exception {
        when(customers.findById(CUSTOMER_B_ID)).thenReturn(Optional.of(customer(CUSTOMER_B_ID)));

        mvc.perform(get("/api/v1/customers/{id}", CUSTOMER_B_ID).header("Authorization", "Bearer advisor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_B_ID.toString()));
    }

    private static Customer customer(UUID id) {
        return Customer.restore(
                id, TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999", true, 0, null, null);
    }
}
