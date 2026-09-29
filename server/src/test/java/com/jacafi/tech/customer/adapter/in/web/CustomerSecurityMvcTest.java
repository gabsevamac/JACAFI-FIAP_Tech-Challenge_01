package com.jacafi.tech.customer.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.customer.adapter.in.web.controller.CustomerController;
import com.jacafi.tech.customer.application.port.CustomerIdentityRepositoryPort;
import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.config.CustomerConfiguration;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.support.TestSecurityConfiguration;
import com.jacafi.tech.support.TestTokens;

@WebMvcTest(CustomerController.class)
@Import({
    CustomerConfiguration.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    TestSecurityConfiguration.class
})
class CustomerSecurityMvcTest {

    private static final UUID CUSTOMER_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final String CUSTOMER_A_SUBJECT = "10000000-0000-0000-0000-000000000001";

    private static final String CUSTOMER_A_BEARER = "Bearer " + TestTokens.customer(CUSTOMER_A_SUBJECT, "customer-a");
    private static final String EMPLOYEE_BEARER = TestTokens.employeeBearer("employee");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerIdentityPort customerIdentities;

    @MockitoBean
    private CustomerIdentityRepositoryPort customerIdentityRepository;

    @MockitoBean
    private CustomerRepositoryPort customers;

    @BeforeEach
    void setUp() {
        when(customerIdentities.customerIdBySubject(CUSTOMER_A_SUBJECT)).thenReturn(Optional.of(CUSTOMER_A_ID));
    }

    @Test
    void customerCannotReadOrUpdateAnotherCustomerById() throws Exception {
        mvc.perform(get("/api/v1/customers/{id}", CUSTOMER_B_ID).header("Authorization", CUSTOMER_A_BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        mvc.perform(patch("/api/v1/customers/{id}", CUSTOMER_B_ID)
                        .header("Authorization", CUSTOMER_A_BEARER)
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
        when(customers.findById(CUSTOMER_A_ID)).thenReturn(Optional.of(customer(CUSTOMER_A_ID)));
        when(customers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(get("/api/v1/customers/me").header("Authorization", CUSTOMER_A_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_A_ID.toString()));

        mvc.perform(patch("/api/v1/customers/me")
                        .header("Authorization", CUSTOMER_A_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria da Silva","email":"nova@example.com","phone":"11888888888"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"));
    }

    @Test
    void customerWithoutALinkedCustomerRecordIsDenied() throws Exception {
        String orphan = "Bearer " + TestTokens.customer("99999999-9999-9999-9999-999999999999", "orphan");
        when(customerIdentities.customerIdBySubject("99999999-9999-9999-9999-999999999999"))
                .thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/customers/me").header("Authorization", orphan))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        verifyNoInteractions(customers);
    }

    @Test
    void employeeCanUseOperationalCustomerLookup() throws Exception {
        when(customers.findById(CUSTOMER_B_ID)).thenReturn(Optional.of(customer(CUSTOMER_B_ID)));

        mvc.perform(get("/api/v1/customers/{id}", CUSTOMER_B_ID).header("Authorization", EMPLOYEE_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_B_ID.toString()));
    }

    @Test
    void onlyAnEmployeeCanLinkACustomerIdentity() throws Exception {
        when(customers.findById(CUSTOMER_B_ID)).thenReturn(Optional.of(customer(CUSTOMER_B_ID)));

        mvc.perform(put("/api/v1/customers/{id}/identity", CUSTOMER_B_ID)
                        .header("Authorization", CUSTOMER_A_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId":"20000000-0000-0000-0000-000000000009"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/customers/{id}/identity", CUSTOMER_B_ID)
                        .header("Authorization", EMPLOYEE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId":"20000000-0000-0000-0000-000000000009"}
                                """))
                .andExpect(status().isNoContent());

        verify(customerIdentityRepository).link("20000000-0000-0000-0000-000000000009", CUSTOMER_B_ID);
    }

    private static Customer customer(UUID id) {
        return Customer.restore(
                id, TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999", true, 0, null, null);
    }
}
