package com.jacafi.tech.client.controller;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientAlreadyExistsException;
import com.jacafi.tech.client.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientControllerTest {

    private ClientService clientService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        clientService = mock(ClientService.class);
        var controller = new ClientController(clientService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ClientExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createsAClient() throws Exception {
        when(clientService.create(any(), any(), any(), any(), any(), any())).thenReturn(client());

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personType": "INDIVIDUAL",
                                  "taxIdentifier": "529.982.247-25",
                                  "name": "Maria",
                                  "email": "maria@example.com",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxIdentifier").value("52998224725"));
    }

    @Test
    void mapsDuplicateClientsToConflict() throws Exception {
        when(clientService.create(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ClientAlreadyExistsException());

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personType": "INDIVIDUAL",
                                  "taxIdentifier": "52998224725",
                                  "name": "Maria",
                                  "email": "maria@example.com",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A client with this tax identifier already exists"));
    }

    private Client client() {
        return Client.create(
                Party.create("Maria", null, TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725")),
                "maria@example.com",
                "11999999999");
    }
}
