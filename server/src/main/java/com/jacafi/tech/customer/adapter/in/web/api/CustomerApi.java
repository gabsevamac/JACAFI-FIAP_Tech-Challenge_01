package com.jacafi.tech.customer.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.customer.adapter.in.web.dto.CreateCustomerRequest;
import com.jacafi.tech.customer.adapter.in.web.dto.CustomerResponse;
import com.jacafi.tech.customer.adapter.in.web.dto.LinkCustomerIdentityRequest;
import com.jacafi.tech.customer.adapter.in.web.dto.UpdateCustomerRequest;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.application.PageResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customers", description = "Customer administration and self-service profile")
@SecurityRequirement(name = "bearer-jwt")
public interface CustomerApi {

    @Operation(summary = "Register a customer")
    ResponseEntity<CustomerResponse> create(CreateCustomerRequest request);

    @Operation(summary = "Find a customer by identifier")
    CustomerResponse findById(UUID id);

    @Operation(summary = "Find a customer by CPF or CNPJ")
    CustomerResponse findByTaxId(String taxId);

    @Operation(summary = "List customers")
    PageResult<CustomerResponse> list(Boolean active, PageParameters paging);

    @Operation(summary = "Update a customer")
    CustomerResponse update(UUID id, UpdateCustomerRequest request);

    @Operation(summary = "Deactivate a customer")
    ResponseEntity<Void> deactivate(UUID id);

    @Operation(
            summary = "Link a customer to an identity provider subject",
            description = "Binds the Keycloak subject that signs in as this customer")
    ResponseEntity<Void> linkIdentity(UUID id, LinkCustomerIdentityRequest request);

    @Operation(summary = "Read the authenticated customer's profile")
    CustomerResponse me();

    @Operation(summary = "Update the authenticated customer's profile")
    CustomerResponse updateMe(UpdateCustomerRequest request);
}
