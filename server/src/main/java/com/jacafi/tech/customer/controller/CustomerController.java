package com.jacafi.tech.customer.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.customer.dto.CreateCustomerRequest;
import com.jacafi.tech.customer.dto.CustomerResponse;
import com.jacafi.tech.customer.dto.PageResponse;
import com.jacafi.tech.customer.dto.UpdateCustomerRequest;
import com.jacafi.tech.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Create a customer")
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        var customer = customerService.create(
                request.taxId(), request.name(), request.tradeName(), request.email(), request.phone());
        return ResponseEntity.created(URI.create("/api/v1/customers/" + customer.getId()))
                .body(CustomerResponse.from(customer));
    }

    @Operation(summary = "Find a customer by id")
    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return CustomerResponse.from(customerService.findById(id));
    }

    @Operation(
            summary = "Find a customer by CPF or CNPJ",
            description =
                    "Which of the two the value is comes from the value itself, so there is " + "no type to declare.")
    @GetMapping("/lookup")
    public CustomerResponse findByTaxId(@RequestParam String taxId) {
        return CustomerResponse.from(customerService.findByTaxId(taxId));
    }

    @Operation(summary = "List customers")
    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(customerService.list(active, pageable).map(CustomerResponse::from));
    }

    @Operation(summary = "Update a customer")
    @PatchMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.from(
                customerService.update(id, request.name(), request.tradeName(), request.email(), request.phone()));
    }

    @Operation(summary = "Deactivate a customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        customerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
