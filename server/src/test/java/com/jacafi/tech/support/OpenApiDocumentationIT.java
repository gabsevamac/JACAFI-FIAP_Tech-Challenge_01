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

/**
 * The OpenAPI document is a project requirement, and two things can silently break it: the
 * springdoc paths falling behind authentication, and an annotation that fails to resolve.
 *
 * <p>Reachable without a token on purpose — the Swagger UI has to fetch this document before any
 * token exists.
 */
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

        // Every documented operation of the slice, so a dropped annotation is caught.
        assertThat(document)
                .contains("Register a vehicle")
                .contains("Find a vehicle by identifier")
                .contains("Find a vehicle by license plate")
                .contains("List a customer's vehicles")
                .contains("List the authenticated customer's vehicles")
                .contains("Update a vehicle")
                .contains("Logically remove a vehicle");
    }

    /**
     * The documentation annotations live on the VehicleApi interface, not on the controller. That
     * only produces a document if springdoc resolves annotations up the method hierarchy — so it
     * is asserted rather than assumed, at every level the interface declares one: the type, the
     * operation, its responses, and its parameters.
     */
    @Test
    @DisplayName("annotations declared on the api interface reach the document")
    void resolvesAnnotationsFromTheInterface() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Type level: @Tag and @SecurityRequirement sit on the interface.
                .andExpect(jsonPath("$.tags[?(@.name == 'Vehicles')]").exists())
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.tags[0]").value("Vehicles"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.security[0]['bearer-jwt']")
                        .exists())
                // Operation level.
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.summary").value("Register a vehicle"))
                // Response level, including the ones with no body.
                .andExpect(jsonPath("$.paths['/api/v1/vehicles'].post.responses.409.description")
                        .value("License plate already registered to an active vehicle"))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{vehicleId}'].delete.responses.204.description")
                        .value("Removed"))
                // Parameter level: @Parameter on an interface method argument, merged with the
                // @RequestParam and @PathVariable that stayed on the implementation.
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
