package com.jacafi.tech.laboroperation.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.laboroperation.adapter.in.web.dto.CreateLaborOperationRequest;
import com.jacafi.tech.laboroperation.adapter.in.web.dto.LaborOperationResponse;
import com.jacafi.tech.laboroperation.adapter.in.web.dto.UpdateLaborOperationRequest;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.application.PageResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Labor operations", description = "Labor operations offered by the workshop, with base prices")
@SecurityRequirement(name = "bearer-jwt")
public interface LaborOperationApi {
    @Operation(summary = "Create a labor operation")
    ResponseEntity<LaborOperationResponse> create(CreateLaborOperationRequest request);

    @Operation(summary = "Find an active labor operation")
    LaborOperationResponse findById(UUID id);

    @Operation(summary = "List active labor operations")
    PageResult<LaborOperationResponse> list(PageParameters paging);

    @Operation(summary = "Update a labor operation")
    LaborOperationResponse update(UUID id, UpdateLaborOperationRequest request);

    @Operation(summary = "Deactivate a labor operation")
    ResponseEntity<Void> deactivate(UUID id);
}
