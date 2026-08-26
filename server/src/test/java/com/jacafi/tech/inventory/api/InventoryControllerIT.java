package com.jacafi.tech.inventory.api;

import com.jacafi.tech.auth.JwtService;
import com.jacafi.tech.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end over the real stack: HTTP, security filter, use case, Hibernate, Postgres.
 *
 * <p>The token is minted for the admin seeded by V2, so the authentication filter resolves a real
 * principal from the database — the same path a client takes.
 */
@AutoConfigureMockMvc
class InventoryControllerIT extends AbstractIntegrationTest {

    private static final String ITEMS = "/api/v1/inventory/items";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String bearerToken;
    private UUID serviceOrderId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE inventory_reservations, inventory_audit_entries, inventory_items");
        bearerToken = "Bearer " + jwtService.generateToken("admin");
        serviceOrderId = UUID.randomUUID();
    }

    private String registrationBody(String name, String type, int initialQuantity) {
        return """
                {"name":"%s","type":"%s","unitPrice":49.90,"initialQuantity":%d}
                """.formatted(name, type, initialQuantity);
    }

    private UUID registerItem(String name, int initialQuantity) throws Exception {
        String body = mockMvc.perform(post(ITEMS)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(name, "PART", initialQuantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }

    private void reserve(UUID itemId, int quantity) throws Exception {
        mockMvc.perform(post(ITEMS + "/" + itemId + "/reservations")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOrderId":"%s","quantity":%d}
                                """.formatted(serviceOrderId, quantity)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST registers, answers 201 with a Location header, and audits the write")
    void registersAMaterial() throws Exception {
        mockMvc.perform(post(ITEMS)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("Filtro de óleo", "PART", 12)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Filtro de óleo"))
                .andExpect(jsonPath("$.type").value("PART"))
                .andExpect(jsonPath("$.quantityOnHand").value(12))
                .andExpect(jsonPath("$.quantityReserved").value(0))
                .andExpect(jsonPath("$.quantityAvailable").value(12));

        List<Map<String, Object>> trail = jdbcTemplate.queryForList(
                "SELECT operation, actor, service_order_id, quantity FROM inventory_audit_entries");
        assertThat(trail).hasSize(1);
        assertThat(trail.getFirst()).containsEntry("operation", "REGISTERED")
                .containsEntry("actor", "admin")
                .containsEntry("service_order_id", null)
                .containsEntry("quantity", null);
    }

    @Test
    @DisplayName("a duplicate name is 409 problem+json, whatever the spacing and the case")
    void rejectsDuplicateName() throws Exception {
        registerItem("Filtro de óleo", 1);

        mockMvc.perform(post(ITEMS)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("  filtro   DE óleo ", "SUPPLY", 1)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("GET by identifier returns the balance and the open reservations")
    void readsAnItem() throws Exception {
        UUID itemId = registerItem("Correia dentada", 10);
        reserve(itemId, 3);

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.quantityReserved").value(3))
                .andExpect(jsonPath("$.quantityAvailable").value(7))
                .andExpect(jsonPath("$.reservations[0].serviceOrderId").value(serviceOrderId.toString()))
                .andExpect(jsonPath("$.reservations[0].quantity").value(3));
    }

    @Test
    @DisplayName("GET lists alphabetically and narrows to one material type")
    void listsTheCatalogue() throws Exception {
        registerItem("Filtro de óleo", 1);
        mockMvc.perform(post(ITEMS)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("Óleo 5W30", "SUPPLY", 20)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ITEMS).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Filtro de óleo"));

        mockMvc.perform(get(ITEMS).param("type", "SUPPLY").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Óleo 5W30"));
    }

    @Test
    @DisplayName("a page size beyond the cap is a client error, not a full table scan")
    void refusesAnUnboundedPage() throws Exception {
        mockMvc.perform(get(ITEMS).param("size", "1000").header("Authorization", bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("PUT corrects name and price and audits the correction")
    void updatesAMaterial() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 4);

        mockMvc.perform(put(ITEMS + "/" + itemId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Filtro de óleo sintético","unitPrice":54.90}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Filtro de óleo sintético"))
                .andExpect(jsonPath("$.unitPrice").value(54.90))
                // The balance is untouched: a correction is not a stock movement.
                .andExpect(jsonPath("$.quantityOnHand").value(4));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_audit_entries WHERE operation = 'UPDATED'", Long.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a price with more decimals than money has is 400")
    void rejectsUnchargeablePrice() throws Exception {
        mockMvc.perform(post(ITEMS)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Óleo 5W30","type":"SUPPLY","unitPrice":39.999,"initialQuantity":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("DELETE takes the item out of the catalogue, and it answers nothing afterwards")
    void removesAMaterial() throws Exception {
        UUID itemId = registerItem("Correia dentada", 2);

        mockMvc.perform(delete(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());

        // The row survives, so the ledger still points at something.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_items WHERE id = ?", Long.class, itemId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("the name of a removed item is free again, which the partial unique index allows")
    void freesTheNameOnRemoval() throws Exception {
        UUID itemId = registerItem("Correia dentada", 2);
        mockMvc.perform(delete(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        UUID replacement = registerItem("Correia dentada", 5);

        assertThat(replacement).isNotEqualTo(itemId);
    }

    @Test
    @DisplayName("replenishing raises the balance and leaves a ledger line with no order on it")
    void replenishesStock() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 4);

        mockMvc.perform(post(ITEMS + "/" + itemId + "/replenishments")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":6}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.quantityAvailable").value(10));

        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT quantity, service_order_id FROM inventory_audit_entries WHERE operation = 'REPLENISHED'");
        assertThat(entry).containsEntry("quantity", 6).containsEntry("service_order_id", null);
    }

    @Test
    @DisplayName("reserving holds units without moving stock, and names the order that holds them")
    void reservesForAnOrder() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);

        mockMvc.perform(post(ITEMS + "/" + itemId + "/reservations")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOrderId":"%s","quantity":4}
                                """.formatted(serviceOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.quantityReserved").value(4))
                .andExpect(jsonPath("$.quantityAvailable").value(6));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT service_order_id, quantity FROM inventory_reservations");
        assertThat(row).containsEntry("service_order_id", serviceOrderId)
                .containsEntry("quantity", 4);
    }

    @Test
    @DisplayName("reserving twice for one order enlarges the single claim it already holds")
    void enlargesTheReservationOfTheSameOrder() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);
        reserve(itemId, 2);
        reserve(itemId, 3);

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservations.length()").value(1))
                .andExpect(jsonPath("$.reservations[0].quantity").value(5))
                .andExpect(jsonPath("$.quantityAvailable").value(5));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_reservations", Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("asking for more than is available is 409 and says how short it is")
    void refusesToOverpromise() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 5);
        reserve(itemId, 4);

        mockMvc.perform(post(ITEMS + "/" + itemId + "/reservations")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOrderId":"%s","quantity":2}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.available").value(1));
    }

    @Test
    @DisplayName("releasing gives the units back and deletes the reservation row")
    void releasesAReservation() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);
        reserve(itemId, 4);

        mockMvc.perform(delete(ITEMS + "/" + itemId + "/reservations/" + serviceOrderId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.quantityAvailable").value(10))
                .andExpect(jsonPath("$.reservations.length()").value(0));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_reservations", Long.class)).isZero();
    }

    @Test
    @DisplayName("withdrawing takes the reserved units off the shelf and records the baixa")
    void withdrawsAgainstAReservation() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);
        reserve(itemId, 4);

        mockMvc.perform(post(ITEMS + "/" + itemId + "/withdrawals")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOrderId":"%s"}
                                """.formatted(serviceOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.serviceOrderId").value(serviceOrderId.toString()));

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(jsonPath("$.quantityOnHand").value(6))
                .andExpect(jsonPath("$.quantityAvailable").value(6))
                .andExpect(jsonPath("$.reservations.length()").value(0));

        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT service_order_id, quantity FROM inventory_audit_entries WHERE operation = 'WITHDRAWN'");
        assertThat(entry).containsEntry("service_order_id", serviceOrderId)
                .containsEntry("quantity", 4);
    }

    @Test
    @DisplayName("no reservation means no approved order, so the withdrawal is refused with 404")
    void refusesToWithdrawWithoutAReservation() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);

        mockMvc.perform(post(ITEMS + "/" + itemId + "/withdrawals")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceOrderId":"%s"}
                                """.formatted(serviceOrderId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(jsonPath("$.quantityOnHand").value(10));
    }

    @Test
    @DisplayName("an item some order is counting on cannot be removed")
    void refusesToRemoveAnItemWithOpenReservations() throws Exception {
        UUID itemId = registerItem("Filtro de óleo", 10);
        reserve(itemId, 1);

        mockMvc.perform(delete(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get(ITEMS + "/" + itemId).header("Authorization", bearerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("every endpoint of the slice requires a JWT")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ITEMS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ITEMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("Filtro de óleo", "PART", 1)))
                .andExpect(status().isUnauthorized());
    }
}
