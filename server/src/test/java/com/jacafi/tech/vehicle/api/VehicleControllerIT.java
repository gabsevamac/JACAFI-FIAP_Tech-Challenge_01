package com.jacafi.tech.vehicle.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.auth.JwtService;
import com.jacafi.tech.support.AbstractIntegrationTest;

/**
 * End-to-end over the real stack: HTTP, security filter, use case, Hibernate, Postgres.
 *
 * <p>The token is minted for the admin seeded by V2, so the authentication filter resolves a real
 * principal from the database — the same path a client takes.
 */
@AutoConfigureMockMvc
class VehicleControllerIT extends AbstractIntegrationTest {

    private static final String PLATE = "ABC1D23";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String bearerToken;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE vehicles, vehicle_audit_entries");
        bearerToken = "Bearer " + jwtService.generateToken("admin");
        customerId = UUID.randomUUID();
    }

    private String registrationBody(String plate) {
        return """
                {"licensePlate":"%s","make":"Volkswagen","model":"Gol","modelYear":2020,"customerId":"%s"}
                """.formatted(plate, customerId);
    }

    private UUID registerVehicle(String plate) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(plate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }

    @Test
    @DisplayName("POST registers, answers 201 with a Location header, and audits the write")
    void registersAVehicle() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PLATE)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.licensePlate").value(PLATE))
                .andExpect(jsonPath("$.make").value("Volkswagen"))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));

        List<Map<String, Object>> trail =
                jdbcTemplate.queryForList("SELECT operation, actor FROM vehicle_audit_entries");
        assertThat(trail).hasSize(1);
        assertThat(trail.getFirst()).containsEntry("operation", "REGISTERED").containsEntry("actor", "admin");
    }

    @Test
    @DisplayName("POST normalizes the plate, so abc-1d23 is stored as ABC1D23")
    void normalizesThePlate() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("abc-1d23")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(PLATE));
    }

    @Test
    @DisplayName("a duplicate plate is 409 problem+json, with the plate absent from the body")
    void rejectsDuplicatePlate() throws Exception {
        registerVehicle(PLATE);

        String body = mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PLATE)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(PLATE);
    }

    @Test
    @DisplayName("an invalid plate format is 400, and the body does not echo it")
    void rejectsInvalidPlate() throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("ABCD123")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("ABCD123");
    }

    @Test
    @DisplayName("a model year beyond next year is 400")
    void rejectsModelYearOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate":"XYZ9K87","make":"Fiat","model":"Argo","modelYear":3000,"customerId":"%s"}
                                """.formatted(customerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findsByIdAndReturnsNotFoundForAnUnknownOne() throws Exception {
        UUID id = registerVehicle(PLATE);

        mockMvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(get("/api/v1/vehicles/{id}", UUID.randomUUID()).header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("finds by plate, accepting the caller's formatting")
    void findsByLicensePlate() throws Exception {
        UUID id = registerVehicle(PLATE);

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("licensePlate", "abc 1d23")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void listsByCustomerWithPaging() throws Exception {
        registerVehicle("ABC1D23");
        registerVehicle("DEF2G34");
        registerVehicle("GHI3J45");

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("customerId", customerId.toString())
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("asking for both filters, or neither, is 400 rather than a guess")
    void rejectsAmbiguousQuery() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles").header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/vehicles")
                        .param("licensePlate", PLATE)
                        .param("customerId", customerId.toString())
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesMakeModelAndYear() throws Exception {
        UUID id = registerVehicle(PLATE);

        mockMvc.perform(put("/api/v1/vehicles/{id}", id)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"make":"Chevrolet","model":"Onix","modelYear":2021}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.make").value("Chevrolet"))
                .andExpect(jsonPath("$.modelYear").value(2021))
                // The plate is untouched: it is the business identity.
                .andExpect(jsonPath("$.licensePlate").value(PLATE));
    }

    @Test
    @DisplayName("DELETE keeps the row, erases the plate from it, and frees the plate")
    void removalAnonymizesAndReleasesThePlate() throws Exception {
        UUID id = registerVehicle(PLATE);

        mockMvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        // The row survives, for the service history required by Art. 16 I...
        Map<String, Object> row =
                jdbcTemplate.queryForMap("SELECT license_plate, make, removed_at FROM vehicles WHERE id = ?", id);
        assertThat(row.get("make")).isEqualTo("Volkswagen");
        assertThat(row.get("removed_at")).isNotNull();
        // ...but the plate is gone from it, satisfying Art. 18 VI.
        assertThat(row.get("license_plate").toString()).doesNotContain(PLATE).startsWith("ANON-");

        // Removed vehicles answer no query.
        mockMvc.perform(get("/api/v1/vehicles/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/vehicles").param("licensePlate", PLATE).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());

        // And the plate is available again, which the partial unique index is what allows.
        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PLATE)))
                .andExpect(status().isCreated());
    }

    @Test
    void removingTwiceIsNotFoundTheSecondTime() throws Exception {
        UUID id = registerVehicle(PLATE);

        mockMvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/vehicles/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("every endpoint answers 401 problem+json without a JWT")
    void rejectsRequestsWithoutAJwt() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(PLATE)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/v1/vehicles/{id}", id)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/vehicles").param("licensePlate", PLATE)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"make":"Chevrolet","model":"Onix","modelYear":2021}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/vehicles/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a malformed token is rejected as well, not treated as anonymous")
    void rejectsAMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/{id}", UUID.randomUUID()).header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }
}
