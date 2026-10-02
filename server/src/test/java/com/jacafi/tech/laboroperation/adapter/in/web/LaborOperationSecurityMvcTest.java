package com.jacafi.tech.laboroperation.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.laboroperation.adapter.in.web.controller.LaborOperationController;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.config.LaborOperationConfiguration;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.config.TimeConfiguration;
import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.support.TestSecurityConfiguration;
import com.jacafi.tech.support.TestTokens;

@WebMvcTest(LaborOperationController.class)
@Import({
    LaborOperationConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    TestSecurityConfiguration.class
})
class LaborOperationSecurityMvcTest {

    private static final String CUSTOMER_BEARER =
            "Bearer " + TestTokens.customer("10000000-0000-0000-0000-000000000001", "customer");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerIdentityPort customerIdentities;

    @MockitoBean
    private LaborOperationRepository operations;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @Test
    void customerCannotAccessLaborOperations() throws Exception {
        mvc.perform(get("/api/v1/labor-operations").header("Authorization", CUSTOMER_BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC-002"));

        verifyNoInteractions(operations, auditTrail);
    }
}
