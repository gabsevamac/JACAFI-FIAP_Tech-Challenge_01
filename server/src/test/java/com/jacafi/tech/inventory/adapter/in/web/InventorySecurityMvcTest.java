package com.jacafi.tech.inventory.adapter.in.web;

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
import com.jacafi.tech.inventory.adapter.in.web.controller.InventoryController;
import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.application.port.InventoryQueryPort;
import com.jacafi.tech.inventory.config.InventoryConfiguration;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.config.TimeConfiguration;
import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.support.TestSecurityConfiguration;
import com.jacafi.tech.support.TestTokens;

@WebMvcTest(InventoryController.class)
@Import({
    InventoryConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    TestSecurityConfiguration.class
})
class InventorySecurityMvcTest {

    private static final String CUSTOMER_BEARER =
            "Bearer " + TestTokens.customer("10000000-0000-0000-0000-000000000001", "customer");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerIdentityPort customerIdentities;

    @MockitoBean
    private InventoryItemRepositoryPort items;

    @MockitoBean
    private InventoryQueryPort queries;

    @MockitoBean
    private InventoryAuditLedgerPort ledger;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @Test
    void customerCannotAccessInventoryCatalogue() throws Exception {
        mvc.perform(get("/api/v1/inventory/items").header("Authorization", CUSTOMER_BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));
        verifyNoInteractions(items, queries, ledger, auditTrail);
    }
}
