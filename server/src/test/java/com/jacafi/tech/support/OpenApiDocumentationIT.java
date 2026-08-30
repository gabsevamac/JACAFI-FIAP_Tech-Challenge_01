package com.jacafi.tech.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class OpenApiDocumentationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the document is served without a JWT and describes the vehicle endpoints")
    void servesTheOpenApiDocument() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SINATES"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{vehicleId}']").exists())
                .andExpect(
                        jsonPath("$.components.securitySchemes['bearer-jwt']").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(document)
                .contains("Register a vehicle")
                .contains("Find a vehicle by identifier")
                .contains("Find a vehicle by license plate")
                .contains("List a customer's vehicles")
                .contains("List the authenticated customer's vehicles")
                .contains("Update a vehicle")
                .contains("Logically remove a vehicle");
    }

    @Test
    @DisplayName("annotations declared on the api interface reach the document")
    void resolvesAnnotationsFromTheInterface() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == 'Vehicles')]").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.tags[0]").value("Vehicles"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.security[0]['bearer-jwt']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.summary").value("Register a vehicle"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.responses.409.description")
                        .value("License plate already registered to an active vehicle"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{vehicleId}'].delete.responses.204.description")
                        .value("Removed"))
                .andExpect(jsonPath(
                                "$.paths['/api/v1/vehicles/lookup'].get.parameters[?(@.name == 'licensePlate')].description")
                        .value("Exact plate, in either layout; separators are ignored"))
                .andExpect(jsonPath(
                                "$.paths['/api/v1/vehicles/{vehicleId}'].get.parameters[?(@.name == 'vehicleId')].description")
                        .value("Identifier assigned at registration"));
    }

    @Test
    @DisplayName("the Swagger UI itself is reachable without a JWT")
    void servesTheSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("the readiness probe is public and reports application health")
    void servesReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
