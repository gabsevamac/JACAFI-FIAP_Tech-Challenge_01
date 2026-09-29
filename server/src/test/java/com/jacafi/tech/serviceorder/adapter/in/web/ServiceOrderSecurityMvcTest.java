package com.jacafi.tech.serviceorder.adapter.in.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import com.jacafi.tech.auth.application.port.UserAccountRepository;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.config.SecurityConfig;
import com.jacafi.tech.inventory.application.port.InventoryItemRepository;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.serviceorder.adapter.in.web.controller.ServiceOrderController;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.config.ServiceOrderConfiguration;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxPublisher;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.config.TimeConfiguration;
import com.jacafi.tech.vehicle.application.port.VehicleRepository;

@WebMvcTest(ServiceOrderController.class)
@Import({
    ServiceOrderConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    JwtAuthenticationFilter.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    SpringSecurityCurrentAuthenticatedUserAdapter.class
})
class ServiceOrderSecurityMvcTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID SERVICE_ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccessTokenPort accessTokens;

    @MockitoBean
    private UserAccountRepository accounts;

    @MockitoBean
    private ServiceOrderRepository orders;

    @MockitoBean
    private VehicleRepository vehicles;

    @MockitoBean
    private LaborOperationRepository laborOperations;

    @MockitoBean
    private InventoryItemRepository inventory;

    @MockitoBean
    private ReserveInventoryStockService reserveInventory;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @MockitoBean
    private EventOutboxPublisher eventOutbox;

    @BeforeEach
    void setUp() {
        when(accessTokens.parseSubject("technician-token")).thenReturn("technician");
        when(accounts.findByUsername("technician"))
                .thenReturn(Optional.of(account("technician", Role.TECHNICIAN, null)));
        when(accessTokens.parseSubject("customer-token")).thenReturn("customer");
        when(accounts.findByUsername("customer"))
                .thenReturn(Optional.of(account("customer", Role.CUSTOMER, CUSTOMER_ID)));
    }

    @Test
    void technicianCanUpdateAnOperationalStatus() throws Exception {
        ServiceOrder order = inProgressOrder();
        when(orders.findById(SERVICE_ORDER_ID)).thenReturn(Optional.of(order));

        mvc.perform(patch("/api/v1/service-orders/{serviceOrderId}/status", SERVICE_ORDER_ID)
                        .header("Authorization", "Bearer technician-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(orders).save(order);
    }

    @Test
    void customerCannotUpdateAServiceOrderStatus() throws Exception {
        mvc.perform(patch("/api/v1/service-orders/{serviceOrderId}/status", SERVICE_ORDER_ID)
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC-002"));

        verifyNoInteractions(orders);
        verify(auditTrail, never()).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void customerCanApproveTheirEstimate() throws Exception {
        ServiceOrder order = orderAwaitingApproval();
        when(orders.findById(SERVICE_ORDER_ID)).thenReturn(Optional.of(order));

        mvc.perform(post(
                                "/api/v1/service-orders/{serviceOrderId}/estimates/{estimateId}/approval",
                                SERVICE_ORDER_ID,
                                order.estimates().getFirst().id())
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"approval-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(orders).save(order);
    }

    @Test
    void customerCanRejectTheirEstimate() throws Exception {
        ServiceOrder order = orderAwaitingApproval();
        when(orders.findById(SERVICE_ORDER_ID)).thenReturn(Optional.of(order));

        mvc.perform(post(
                                "/api/v1/service-orders/{serviceOrderId}/estimates/{estimateId}/rejection",
                                SERVICE_ORDER_ID,
                                order.estimates().getFirst().id())
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"rejection-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(orders).save(order);
    }

    private static UserAccount account(String username, Role role, UUID customerId) {
        return UserAccount.restore(UUID.randomUUID(), username, "hash", Set.of(role), customerId, true);
    }

    private static ServiceOrder orderAwaitingApproval() {
        ServiceOrder order =
                ServiceOrder.open(SERVICE_ORDER_ID, CUSTOMER_ID, UUID.randomUUID(), "Engine noise", "advisor", CLOCK);
        order.startDiagnosis("advisor", CLOCK);
        order.generateEstimate("advisor", CLOCK);
        return order;
    }

    private static ServiceOrder inProgressOrder() {
        ServiceOrder order = ServiceOrder.open(
                SERVICE_ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), "Engine noise", "advisor", CLOCK);
        order.startDiagnosis("advisor", CLOCK);
        order.generateEstimate("advisor", CLOCK);
        order.approveEstimate(order.estimates().getFirst().id(), "approval-1", "advisor", CLOCK);
        return order;
    }
}
