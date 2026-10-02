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
import com.jacafi.tech.shared.security.CustomerIdentityPort;
import com.jacafi.tech.support.TestSecurityConfiguration;
import com.jacafi.tech.support.TestTokens;
import com.jacafi.tech.vehicle.application.port.VehicleRepository;

@WebMvcTest(ServiceOrderController.class)
@Import({
    ServiceOrderConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    TestSecurityConfiguration.class
})
class ServiceOrderSecurityMvcTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID SERVICE_ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final String CUSTOMER_SUBJECT = "10000000-0000-0000-0000-000000000001";

    private static final String EMPLOYEE_BEARER = TestTokens.employeeBearer("employee");
    private static final String CUSTOMER_BEARER = "Bearer " + TestTokens.customer(CUSTOMER_SUBJECT, "customer");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerIdentityPort customerIdentities;

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
        when(customerIdentities.customerIdBySubject(CUSTOMER_SUBJECT)).thenReturn(Optional.of(CUSTOMER_ID));
    }

    @Test
    void employeeCanUpdateAnOperationalStatus() throws Exception {
        ServiceOrder order = inProgressOrder();
        when(orders.findById(SERVICE_ORDER_ID)).thenReturn(Optional.of(order));

        mvc.perform(patch("/api/v1/service-orders/{serviceOrderId}/status", SERVICE_ORDER_ID)
                        .header("Authorization", EMPLOYEE_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(orders).save(order);
    }

    @Test
    void customerCannotUpdateAServiceOrderStatus() throws Exception {
        mvc.perform(patch("/api/v1/service-orders/{serviceOrderId}/status", SERVICE_ORDER_ID)
                        .header("Authorization", CUSTOMER_BEARER)
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
                        .header("Authorization", CUSTOMER_BEARER)
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
                        .header("Authorization", CUSTOMER_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"rejection-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(orders).save(order);
    }

    private static ServiceOrder orderAwaitingApproval() {
        ServiceOrder order =
                ServiceOrder.open(SERVICE_ORDER_ID, CUSTOMER_ID, UUID.randomUUID(), "Engine noise", "employee", CLOCK);
        order.startDiagnosis("employee", CLOCK);
        order.generateEstimate("employee", CLOCK);
        return order;
    }

    private static ServiceOrder inProgressOrder() {
        ServiceOrder order =
                ServiceOrder.open(SERVICE_ORDER_ID, CUSTOMER_ID, UUID.randomUUID(), "Engine noise", "employee", CLOCK);
        order.startDiagnosis("employee", CLOCK);
        order.generateEstimate("employee", CLOCK);
        order.approveEstimate(order.estimates().getFirst().id(), "approval-1", "employee", CLOCK);
        return order;
    }
}
