package com.jacafi.tech.customer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///jacafi-customer-api",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=jacafi",
        "spring.datasource.password=jacafi"
})
@AutoConfigureMockMvc(addFilters = false)
// Sem o perfil de teste, jwt.secret fica sem valor e o contexto nao sobe: application.yaml
// resolve ${JWT_SECRET} na criacao do JwtService.
@ActiveProfiles("test")
class CustomerApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void executesTheCompleteCustomerFlow() throws Exception {
        var location = mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxId": "00.000.000/E08G-12",
                                  "name": "Oficina Jacafi Ltda",
                                  "tradeName": "Jacafi",
                                  "email": "contato@jacafi.com.br",
                                  "phone": "1133334444"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.taxId").value("00000000E08G12"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Oficina Jacafi Ltda"));

        mvc.perform(get("/api/v1/customers/lookup")
                        .param("taxId", "00.000.000/E08G-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeName").value("Jacafi"));

        mvc.perform(patch(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Oficina Jacafi Tecnologia Ltda",
                                  "tradeName": "Jacafi Tech",
                                  "email": "novo@jacafi.com.br",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Oficina Jacafi Tecnologia Ltda"));

        mvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/customers").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].taxId", hasItem("00000000E08G12")));
    }

    @Test
    void publishesTheOpenApiDescription() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/customers']").exists());
    }
}
