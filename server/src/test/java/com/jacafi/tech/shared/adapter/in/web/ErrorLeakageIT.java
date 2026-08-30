package com.jacafi.tech.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.auth.adapter.out.security.JwtTokenAdapter;
import com.jacafi.tech.support.AbstractIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("what an error response must not leak")
class ErrorLeakageIT extends AbstractIntegrationTest {

    private static final String PLATE = "LEK1A23";
    private static final String CPF = "52998224725";
    private static final UUID CUSTOMER_ID = UUID.fromString("a1d4e145-e3f8-4fdc-b84e-8584c564c927");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenAdapter jwtTokenAdapter;

    @Autowired
    private JdbcTemplate jdbc;

    private String bearer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE vehicles, audit_trail CASCADE");
        bearer = "Bearer " + jwtTokenAdapter.issue("dev-admin");
        customerId = CUSTOMER_ID;
        jdbc.update("""
                INSERT INTO customers (id, tax_id, name, email, phone, active, created_at, created_by, updated_at, updated_by, version)
                VALUES (?, '52998224725', 'Integration Test Customer', 'integration-test@example.com', '11999999999', TRUE, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)
                ON CONFLICT (id) DO NOTHING
                """, customerId);
    }

    private String vehicleBody(String plate) {
        return """
                {"licensePlate":"%s","make":"Volkswagen","model":"Gol","modelYear":2020,"customerId":"%s"}
                """.formatted(plate, customerId);
    }

    @Nested
    @DisplayName("a unique index violation on the license plate")
    class DuplicatePlate {

        @Test
        @DisplayName("carries neither the plate nor the index name")
        void leaksNeitherPlateNorIndexName() throws Exception {
            mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody(PLATE)))
                    .andExpect(status().isCreated());

            String body = mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody(PLATE)))
                    .andExpect(status().isConflict())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body)
                    .doesNotContain(PLATE)
                    .doesNotContain("ux_vehicles_license_plate_active")
                    .doesNotContain("license_plate")
                    .doesNotContain("duplicate key")
                    .doesNotContain("constraint");
        }
    }

    @Nested
    @DisplayName("an unexpected failure")
    class Unexpected {

        @Test
        @DisplayName("answers 500 with status, code and traceId, and nothing else")
        void carriesNoStackTrace() throws Exception {

            String body = mockMvc.perform(
                            get("/api/v1/vehicles/" + UUID.randomUUID()).header("Authorization", bearer))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body)
                    .doesNotContain("java.lang")
                    .doesNotContain("com.jacafi")
                    .doesNotContain("at org.springframework")
                    .doesNotContain("Caused by")
                    .doesNotContain(".java:")
                    .doesNotContain("Exception");
        }

        @Test
        @DisplayName("the /error route exposes neither trace nor message")
        void theErrorRouteIsHardened() throws Exception {
            String body = mockMvc.perform(get("/api/v1/rota-que-nao-existe").header("Authorization", bearer))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.code").value("GEN-008"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body).doesNotContain(".java:").doesNotContain("com.jacafi");
        }
    }

    @Nested
    @DisplayName("a validation failure on personal data")
    class PersonalData {

        @Test
        @DisplayName("never echoes the submitted registration")
        void doesNotEchoTheTaxId() throws Exception {
            String body = mockMvc.perform(post("/api/v1/customers")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"taxId":"%s","name":"Maria","email":"maria@example.com","phone":"11999999999"}
                                    """.formatted("11111111111")))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body).doesNotContain("11111111111");
        }

        @Test
        @DisplayName("never echoes the submitted plate")
        void doesNotEchoTheLicensePlate() throws Exception {
            String body = mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody("NAO-E-PLACA")))
                    .andExpect(status().isBadRequest())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body).doesNotContain("NAO-E-PLACA");
        }
    }

    @Nested
    @DisplayName("every error response")
    class EveryResponse {

        @Test
        @DisplayName("a 401 says nothing about whether the user exists")
        void authenticationFailureIsOpaque() throws Exception {
            String body = mockMvc.perform(get("/api/v1/vehicles/" + UUID.randomUUID())
                            .header("Authorization", "Bearer nao-e-um-token"))
                    .andExpect(status().isUnauthorized())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(body)
                    .doesNotContain("expired")
                    .doesNotContain("expirado")
                    .doesNotContain("signature")
                    .doesNotContain("Jwt")
                    .doesNotContain("admin");
        }

        @Test
        @DisplayName("carries the X-Trace-Id header")
        void carriesTheTraceHeader() throws Exception {
            mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody("INVALIDA")))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().exists(TraceIdFilter.HEADER));

            mockMvc.perform(get("/api/v1/vehicles/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists(TraceIdFilter.HEADER))
                    .andExpect(jsonPath("$.code").value("SEG-001"))
                    .andExpect(jsonPath("$.traceId").exists());
        }

        @Test
        @DisplayName("repeats the trace id in the body, and it matches the header")
        void bodyAndHeaderAgree() throws Exception {
            var result = mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody("INVALIDA")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.code").exists())
                    .andReturn();

            String header = result.getResponse().getHeader(TraceIdFilter.HEADER);
            assertThat(result.getResponse().getContentAsString()).contains(header);
        }

        @Test
        @DisplayName("gives a different trace id to each request")
        void traceIdsAreUnique() throws Exception {
            String first = traceOf();
            String second = traceOf();

            assertThat(first).isNotEqualTo(second);
        }

        private String traceOf() throws Exception {
            return mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(vehicleBody("INVALIDA")))
                    .andReturn()
                    .getResponse()
                    .getHeader(TraceIdFilter.HEADER);
        }
    }
}
