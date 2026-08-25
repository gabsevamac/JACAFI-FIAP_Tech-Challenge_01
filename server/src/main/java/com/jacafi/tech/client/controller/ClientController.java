package com.jacafi.tech.client.controller;

import com.jacafi.tech.client.dto.ClientResponse;
import com.jacafi.tech.client.dto.CreateClientRequest;
import com.jacafi.tech.client.dto.PageResponse;
import com.jacafi.tech.client.dto.UpdateClientRequest;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.net.URI;
import java.util.UUID;

@Tag(name = "Clients")
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Create a client")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        var client = clientService.create(
                request.personType(),
                request.taxIdentifier(),
                request.name(),
                request.tradeName(),
                request.email(),
                request.phone());
        return ResponseEntity.created(URI.create("/api/v1/clients/" + client.getId()))
                .body(ClientResponse.from(client));
    }

    @Operation(summary = "Find a client by id")
    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable UUID id) {
        return ClientResponse.from(clientService.findById(id));
    }

    @Operation(summary = "Find a client by CPF or CNPJ")
    @GetMapping("/lookup")
    public ClientResponse findByTaxIdentifier(
            @RequestParam PersonType personType,
            @RequestParam String taxIdentifier) {
        return ClientResponse.from(clientService.findByTaxIdentifier(personType, taxIdentifier));
    }

    @Operation(summary = "List clients")
    @GetMapping
    public PageResponse<ClientResponse> list(
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(clientService.list(active, pageable).map(ClientResponse::from));
    }

    @Operation(summary = "Update a client")
    @PatchMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClientRequest request) {
        return ClientResponse.from(clientService.update(
                id,
                request.name(),
                request.tradeName(),
                request.email(),
                request.phone()));
    }

    @Operation(summary = "Deactivate a client")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
