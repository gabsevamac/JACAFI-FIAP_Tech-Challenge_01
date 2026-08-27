package com.jacafi.tech.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jacafi.tech.auth.adapter.out.security.JwtTokenAdapter;
import com.jacafi.tech.support.AbstractIntegrationTest;
import com.jacafi.tech.support.FixedClockConfiguration;

/**
 * The paging contract, over HTTP, against the real stack.
 *
 * <p>Asserted at the edge rather than on {@code PageParameters} directly, because the guarantees
 * that matter here are about what a client receives: the status code, and what the error body does
 * not contain.
 */
@AutoConfigureMockMvc
// O relogio fixo nao esta aqui por determinismo, e sim para forcar o pior caso da ordenacao:
// com ele todo created_at e literalmente o mesmo valor, entao a ordenacao por registro nao tem
// nada para separar as linhas alem do desempate por identificador.
@Import(FixedClockConfiguration.class)
@DisplayName("the paging contract over HTTP")
class PagingContractIT extends AbstractIntegrationTest {

    private static final String CUSTOMER_VEHICLES = "/api/v1/vehicles";

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

    private void register(String plate) throws Exception {
        mockMvc.perform(post(CUSTOMER_VEHICLES)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate":"%s","make":"Volkswagen","model":"Gol","modelYear":2020,"customerId":"%s"}
                                """.formatted(plate, customerId)))
                .andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("bounds")
    class Bounds {

        @Test
        @DisplayName("a size above the ceiling is refused with 400, not quietly truncated")
        void refusesAnOversizedPage() throws Exception {
            mockMvc.perform(get(CUSTOMER_VEHICLES)
                            .header("Authorization", bearer)
                            .param("customerId", customerId.toString())
                            .param("size", "500"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("a negative page is refused with 400")
        void refusesANegativePage() throws Exception {
            mockMvc.perform(get(CUSTOMER_VEHICLES)
                            .header("Authorization", bearer)
                            .param("customerId", customerId.toString())
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("an unknown sort field")
    class UnknownSortField {

        @Test
        @DisplayName("is refused with 400")
        void isRefused() throws Exception {
            mockMvc.perform(get(CUSTOMER_VEHICLES)
                            .header("Authorization", bearer)
                            .param("customerId", customerId.toString())
                            .param("sort", "campoInexistente"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("leaks neither the field name nor any class or property name")
        void leaksNothing() throws Exception {
            String body = mockMvc.perform(get(CUSTOMER_VEHICLES)
                            .header("Authorization", bearer)
                            .param("customerId", customerId.toString())
                            .param("sort", "campoInexistente"))
                    .andExpect(status().isBadRequest())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // The submitted value, echoed back, confirms to a prober which names are being tested.
            assertThat(body).doesNotContain("campoInexistente");

            // Spring Data's PropertyReferenceException reads "No property 'x' found for type
            // 'VehicleJpaEntity'" and lists the properties that do exist. Letting it reach the
            // client hands over the entity's shape one request at a time.
            assertThat(body)
                    .doesNotContain("VehicleJpaEntity")
                    .doesNotContain("PropertyReferenceException")
                    .doesNotContain("No property")
                    .doesNotContain("licensePlate")
                    .doesNotContain("com.jacafi");
        }
    }

    @Nested
    @DisplayName("consecutive pages")
    class ConsecutivePages {

        @Test
        @DisplayName("neither repeat nor skip a record")
        void areStable() throws Exception {
            // Five vehicles under one fixed clock, so every created_at is literally the same
            // value and the ordering has nothing to separate them by but the tie-breaker.
            //
            // Worth being exact about what this proves, because it is less than it looks.
            // Removing the tie-breaker from SortableFields leaves this test green: Postgres
            // happens to return these five rows in a stable order anyway, on a small table with a
            // sequential scan. Instability is permitted, not guaranteed, so no integration test
            // can force it. What actually guards the tie-breaker is
            // PageParametersTest.appendsTheTieBreaker, which asserts on the resolved criteria and
            // does fail under that mutation.
            //
            // This one guards the contract a client depends on — that walking the pages yields
            // every record exactly once — which is worth asserting even where the mechanism is
            // asserted elsewhere.
            List<String> plates = List.of("PGA1A11", "PGB2B22", "PGC3C33", "PGD4D44", "PGE5E55");
            for (String plate : plates) {
                register(plate);
            }

            List<String> collected = new ArrayList<>();
            collected.addAll(idsOfPage(0, 2));
            collected.addAll(idsOfPage(1, 2));
            collected.addAll(idsOfPage(2, 2));

            assertThat(collected).hasSize(5).doesNotHaveDuplicates();
        }

        private List<String> idsOfPage(int page, int size) throws Exception {
            String body = mockMvc.perform(get(CUSTOMER_VEHICLES)
                            .header("Authorization", bearer)
                            .param("customerId", customerId.toString())
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size))
                            .param("sort", "registeredAt"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            List<String> ids = new ArrayList<>();
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("\"id\":\"([^\"]+)\"").matcher(body);
            while (matcher.find()) {
                ids.add(matcher.group(1));
            }
            return ids;
        }
    }

    @Test
    @DisplayName("the response carries the agreed field names")
    void answersTheAgreedShape() throws Exception {
        register("PGF6F66");

        mockMvc.perform(get(CUSTOMER_VEHICLES)
                        .header("Authorization", bearer)
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                // Spring Data's Page would serialize "pageable", "numberOfElements", "first",
                // "last" and a nested "sort" object. None of that is our contract.
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.numberOfElements").doesNotExist());
    }
}
