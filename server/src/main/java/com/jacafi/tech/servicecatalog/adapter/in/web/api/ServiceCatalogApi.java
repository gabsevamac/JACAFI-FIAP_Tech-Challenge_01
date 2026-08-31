package com.jacafi.tech.servicecatalog.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.servicecatalog.adapter.in.web.dto.CreateServiceCatalogItemRequest;
import com.jacafi.tech.servicecatalog.adapter.in.web.dto.ServiceCatalogItemResponse;
import com.jacafi.tech.servicecatalog.adapter.in.web.dto.UpdateServiceCatalogItemRequest;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.application.PageResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Service catalog", description = "Services offered by the workshop")
@SecurityRequirement(name = "bearer-jwt")
public interface ServiceCatalogApi {
    @Operation(summary = "Create a service catalog item")
    ResponseEntity<ServiceCatalogItemResponse> create(CreateServiceCatalogItemRequest request);

    @Operation(summary = "Find an active service catalog item")
    ServiceCatalogItemResponse findById(UUID id);

    @Operation(summary = "List active service catalog items")
    PageResult<ServiceCatalogItemResponse> list(PageParameters paging);

    @Operation(summary = "Update a service catalog item")
    ServiceCatalogItemResponse update(UUID id, UpdateServiceCatalogItemRequest request);

    @Operation(summary = "Deactivate a service catalog item")
    ResponseEntity<Void> deactivate(UUID id);
}
