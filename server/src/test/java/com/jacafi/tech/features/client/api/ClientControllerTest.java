package com.jacafi.tech.features.client.api;

import com.jacafi.tech.features.client.create.ClientAlreadyExistsException;
import com.jacafi.tech.features.client.create.CreateClientUseCase;
import com.jacafi.tech.features.client.domain.Client;
import com.jacafi.tech.features.client.domain.Party;
import com.jacafi.tech.features.client.domain.PersonType;
import com.jacafi.tech.features.client.domain.TaxIdentifier;
import com.jacafi.tech.features.client.query.FindClientUseCase;
import com.jacafi.tech.features.client.query.ListClientsUseCase;
import com.jacafi.tech.features.client.update.DeactivateClientUseCase;
import com.jacafi.tech.features.client.update.UpdateClientUseCase;
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

    private CreateClientUseCase createClient;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        createClient = mock(CreateClientUseCase.class);
        var controller = new ClientController(
                createClient,
                mock(FindClientUseCase.class),
                mock(ListClientsUseCase.class),
                mock(UpdateClientUseCase.class),
                mock(DeactivateClientUseCase.class));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ClientExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createsAClient() throws Exception {
        when(createClient.execute(any())).thenReturn(client());

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
        when(createClient.execute(any())).thenThrow(new ClientAlreadyExistsException());

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
