package com.jacafi.tech.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jacafi.tech.customer.entity.Customer;
import com.jacafi.tech.customer.entity.TaxId;
import com.jacafi.tech.customer.exception.CustomerAlreadyExistsException;
import com.jacafi.tech.customer.service.CustomerService;
import com.jacafi.tech.shared.adapter.in.web.GlobalExceptionHandler;

class CustomerControllerTest {

    private CustomerService customerService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        customerService = mock(CustomerService.class);
        var controller = new CustomerController(customerService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createsACustomer() throws Exception {
        when(customerService.create(any(), any(), any(), any(), any())).thenReturn(customer());

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxId": "529.982.247-25",
                                  "name": "Maria",
                                  "email": "maria@example.com",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxId").value("52998224725"));
    }

    @Test
    void mapsDuplicateCustomersToConflict() throws Exception {
        when(customerService.create(any(), any(), any(), any(), any())).thenThrow(new CustomerAlreadyExistsException());

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taxId": "52998224725",
                                  "name": "Maria",
                                  "email": "maria@example.com",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um cliente com este CPF ou CNPJ."))
                // O codigo e o que um cliente deve usar para tratar programaticamente: o texto e
                // traduzivel e reescrevivel, CLI-002 nao.
                .andExpect(jsonPath("$.code").value("CLI-002"));
    }

    private Customer customer() {
        return Customer.create(TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999");
    }
}
