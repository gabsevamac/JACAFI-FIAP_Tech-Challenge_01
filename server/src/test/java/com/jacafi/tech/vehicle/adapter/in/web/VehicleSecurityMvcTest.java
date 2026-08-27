package com.jacafi.tech.vehicle.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.config.TimeConfiguration;
import com.jacafi.tech.vehicle.adapter.in.web.controller.VehicleController;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.config.VehicleConfiguration;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;

@WebMvcTest(VehicleController.class)
@Import({
    VehicleConfiguration.class,
    TimeConfiguration.class,
    GlobalExceptionHandler.class,
    JwtAuthenticationFilter.class,
    SecurityConfig.class,
    SecurityProblemDetailHandler.class,
    SpringSecurityCurrentAuthenticatedUserAdapter.class
})
class VehicleSecurityMvcTest {

    private static final UUID CUSTOMER_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID VEHICLE_B_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

    private static final UserAccount CUSTOMER_A = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "customer-a",
            "hash",
            Set.of(Role.CUSTOMER),
            CUSTOMER_A_ID,
            true);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccessTokenPort accessTokens;

    @MockitoBean
    private UserAccountRepositoryPort accounts;

    @MockitoBean
    private VehicleRepositoryPort vehicles;

    @MockitoBean
    private AuditTrailPort auditTrail;

    @BeforeEach
    void setUp() {
        when(accessTokens.parseSubject("customer-a-token")).thenReturn("customer-a");
        when(accounts.findByUsername("customer-a")).thenReturn(Optional.of(CUSTOMER_A));
    }

    @Test
    void customerCannotReadUpdateOrListAnotherCustomersVehicles() throws Exception {
        mvc.perform(get("/api/v1/vehicles/{id}", VEHICLE_B_ID).header("Authorization", "Bearer customer-a-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        mvc.perform(put("/api/v1/vehicles/{id}", VEHICLE_B_ID)
                        .header("Authorization", "Bearer customer-a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"make":"Ford","model":"Ka","modelYear":2020}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        mvc.perform(get("/api/v1/vehicles")
                        .param("customerId", CUSTOMER_B_ID.toString())
                        .header("Authorization", "Bearer customer-a-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEG-002"));

        verifyNoInteractions(vehicles);
    }

    @Test
    void customerListsOnlyVehiclesLinkedToTheAuthenticatedCustomer() throws Exception {
        Vehicle vehicle = Vehicle.restore(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                new LicensePlate("ABC1D23"),
                "Volkswagen",
                "Gol",
                2020,
                CUSTOMER_A_ID,
                0,
                Instant.EPOCH,
                Instant.EPOCH,
                null);
        when(vehicles.findActiveByCustomerId(org.mockito.ArgumentMatchers.eq(CUSTOMER_A_ID), any()))
                .thenReturn(PageResult.of(List.of(vehicle), 0, 20, 1));

        mvc.perform(get("/api/v1/vehicles/me").header("Authorization", "Bearer customer-a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(CUSTOMER_A_ID.toString()));
    }
}
