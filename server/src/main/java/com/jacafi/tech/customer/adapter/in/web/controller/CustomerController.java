package com.jacafi.tech.customer.adapter.in.web.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.customer.adapter.in.web.api.CustomerApi;
import com.jacafi.tech.customer.adapter.in.web.dto.CreateCustomerRequest;
import com.jacafi.tech.customer.adapter.in.web.dto.CustomerResponse;
import com.jacafi.tech.customer.adapter.in.web.dto.LinkCustomerIdentityRequest;
import com.jacafi.tech.customer.adapter.in.web.dto.UpdateCustomerRequest;
import com.jacafi.tech.customer.application.service.DeactivateCustomerService;
import com.jacafi.tech.customer.application.service.FindCustomerByTaxIdService;
import com.jacafi.tech.customer.application.service.FindCustomerService;
import com.jacafi.tech.customer.application.service.GetCurrentCustomerService;
import com.jacafi.tech.customer.application.service.LinkCustomerIdentityService;
import com.jacafi.tech.customer.application.service.ListCustomersService;
import com.jacafi.tech.customer.application.service.RegisterCustomerService;
import com.jacafi.tech.customer.application.service.UpdateCurrentCustomerService;
import com.jacafi.tech.customer.application.service.UpdateCustomerService;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageResult;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController implements CustomerApi {

    private static final SortableFields SORTABLE = SortableFields.of("id", "name", "createdAt", "active");

    private final RegisterCustomerService registerCustomer;
    private final FindCustomerService findCustomer;
    private final FindCustomerByTaxIdService findCustomerByTaxId;
    private final ListCustomersService listCustomers;
    private final UpdateCustomerService updateCustomer;
    private final DeactivateCustomerService deactivateCustomer;
    private final GetCurrentCustomerService getCurrentCustomer;
    private final UpdateCurrentCustomerService updateCurrentCustomer;
    private final LinkCustomerIdentityService linkCustomerIdentity;

    public CustomerController(
            RegisterCustomerService registerCustomer,
            FindCustomerService findCustomer,
            FindCustomerByTaxIdService findCustomerByTaxId,
            ListCustomersService listCustomers,
            UpdateCustomerService updateCustomer,
            DeactivateCustomerService deactivateCustomer,
            GetCurrentCustomerService getCurrentCustomer,
            UpdateCurrentCustomerService updateCurrentCustomer,
            LinkCustomerIdentityService linkCustomerIdentity) {
        this.registerCustomer = registerCustomer;
        this.findCustomer = findCustomer;
        this.findCustomerByTaxId = findCustomerByTaxId;
        this.listCustomers = listCustomers;
        this.updateCustomer = updateCustomer;
        this.deactivateCustomer = deactivateCustomer;
        this.getCurrentCustomer = getCurrentCustomer;
        this.updateCurrentCustomer = updateCurrentCustomer;
        this.linkCustomerIdentity = linkCustomerIdentity;
    }

    @Override
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        var customer = registerCustomer.register(
                request.taxId(), request.name(), request.tradeName(), request.email(), request.phone());
        return ResponseEntity.created(URI.create("/api/v1/customers/" + customer.id()))
                .body(CustomerResponse.from(customer));
    }

    @Override
    @GetMapping("/me")
    public CustomerResponse me() {
        return CustomerResponse.from(getCurrentCustomer.get());
    }

    @Override
    @PatchMapping("/me")
    public CustomerResponse updateMe(@Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.from(
                updateCurrentCustomer.update(request.name(), request.tradeName(), request.email(), request.phone()));
    }

    @Override
    @GetMapping("/lookup")
    public CustomerResponse findByTaxId(@RequestParam String taxId) {
        return CustomerResponse.from(findCustomerByTaxId.find(taxId));
    }

    @Override
    @GetMapping
    public PageResult<CustomerResponse> list(@RequestParam(required = false) Boolean active, PageParameters paging) {
        return listCustomers.list(active, paging.toQuery(SORTABLE)).map(CustomerResponse::from);
    }

    @Override
    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return CustomerResponse.from(findCustomer.find(id));
    }

    @Override
    @PatchMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.from(
                updateCustomer.update(id, request.name(), request.tradeName(), request.email(), request.phone()));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateCustomer.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/{id}/identity")
    public ResponseEntity<Void> linkIdentity(
            @PathVariable UUID id, @Valid @RequestBody LinkCustomerIdentityRequest request) {
        linkCustomerIdentity.link(id, request.subjectId());
        return ResponseEntity.noContent().build();
    }
}
