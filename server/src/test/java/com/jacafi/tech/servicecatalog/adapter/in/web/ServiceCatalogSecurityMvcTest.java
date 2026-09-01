package com.jacafi.tech.servicecatalog.adapter.in.web;

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
import com.jacafi.tech.servicecatalog.adapter.in.web.controller.ServiceCatalogController;
import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.config.ServiceCatalogConfiguration;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.config.TimeConfiguration;
import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.support.TestSecurityConfiguration;
import com.jacafi.tech.support.TestTokens;

@WebMvcTest(ServiceCatalogController.class)
@Import({
    ServiceCatalogConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    TestSecurityConfiguration.class
})
class ServiceCatalogSecurityMvcTest {

    private static final String CUSTOMER_BEARER =
            "Bearer " + TestTokens.customer("10000000-0000-0000-0000-000000000001", "customer");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerIdentityPort customerIdentities;

    @MockitoBean
    private ServiceCatalogRepositoryPort items;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @Test
    void customerCannotAccessTheServiceCatalog() throws Exception {
        mvc.perform(get("/api/v1/service-catalog-items").header("Authorization", CUSTOMER_BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        verifyNoInteractions(items, auditTrail);
    }
}
