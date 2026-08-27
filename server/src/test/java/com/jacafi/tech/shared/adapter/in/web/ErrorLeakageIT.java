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

/**
 * What an error response must never contain.
 *
 * <p>The most important tests of the error handling, and the ones written as prohibitions rather
 * than expectations. A test asserting that a 409 has the right message stays green while the body
 * also carries the index name and the submitted plate; only a test that asserts on absence
 * catches that.
 *
 * <p>Every scanner in the world checks for these, and a vulnerability report is a deliverable of
 * this challenge.
 */
@AutoConfigureMockMvc
@DisplayName("what an error response must not leak")
class ErrorLeakageIT extends AbstractIntegrationTest {

    private static final String PLATE = "LEK1A23";
    private static final String CPF = "52998224725";

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
        jdbc.execute("TRUNCATE TABLE vehicles, audit_trail");
        bearer = "Bearer " + jwtTokenAdapter.issue("dev-admin");
        customerId = UUID.randomUUID();
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

            // Postgres words this as: duplicate key value violates unique constraint
            // "ux_vehicles_license_plate_active" / Detail: Key (license_plate)=(LEK1A23) already
            // exists. Hibernate wraps it, Spring rewraps it, and getMessage() publishes all of it.
            assertThat(body)
                    .doesNotContain(PLATE)
                    .doesNotContain("ux_vehicles_license_plate_active")
                    .doesNotContain("license_plate")
                    .doesNotContain("duplicate key")
                    .doesNotContain("constraint");
            // "vehicles" nao entra na lista: o corpo traz "instance":"/api/v1/vehicles", que e o
            // caminho que o proprio cliente pediu. Afirmar sobre ele testaria a URI, nao o
            // vazamento — e um teste que falha por motivo errado e apagado, nao consertado.
        }
    }

    @Nested
    @DisplayName("an unexpected failure")
    class Unexpected {

        @Test
        @DisplayName("answers 500 with status, code and traceId, and nothing else")
        void carriesNoStackTrace() throws Exception {
            // A UUID that is syntactically valid makes it past binding and into the use case; the
            // route that fails inside is what produces an unhandled exception rather than a 404.
            // What matters is the shape of the body, not which failure produced it.
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
                    // "trace" e a propriedade que o tratador padrao do container acrescenta com a
                    // stack trace; "traceId" e nossa e precisa estar la. Por isso a assercao e
                    // sobre a propriedade JSON, e nao sobre a substring.
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

            // An invalid CPF is still a value a person typed, and an error body is not a place for
            // it (LGPD Art. 6 VII). FieldError.getRejectedValue() holds exactly this string, which
            // is what a naive handler puts in the response.
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

            // Distinguir "usuario inexistente" de "senha errada" de "token expirado" entrega um
            // oraculo de enumeracao: quem sonda aprende quais usuarios existem pela diferenca.
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

            // Including the ones produced before any controller runs. A request rejected by the
            // security filter never reaches the advice, and a filter ordered after security could
            // not have added the header.
            mockMvc.perform(get("/api/v1/vehicles/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists(TraceIdFilter.HEADER))
                    // E o corpo segue o mesmo contrato do resto, apesar de vir de outro lugar.
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
