package com.jacafi.tech.vehicle.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.adapter.in.web.dto.RegisterVehicleRequest;
import com.jacafi.tech.vehicle.adapter.in.web.dto.UpdateVehicleRequest;
import com.jacafi.tech.vehicle.adapter.in.web.dto.VehicleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicles", description = "Vehicle registry and customer self-service")
@SecurityRequirement(name = "bearer-jwt")
public interface VehicleApi {

    @Operation(summary = "Register a vehicle")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registered"),
        @ApiResponse(responseCode = "409", description = "License plate already registered to an active vehicle")
    })
    ResponseEntity<VehicleResponse> register(RegisterVehicleRequest request);

    @Operation(summary = "Find a vehicle by identifier")
    VehicleResponse findById(@Parameter(description = "Identifier assigned at registration") UUID vehicleId);

    @Operation(summary = "Find a vehicle by license plate")
    VehicleResponse findByLicensePlate(
            @Parameter(description = "Exact plate, in either layout; separators are ignored") String licensePlate);

    @Operation(summary = "List a customer's vehicles")
    PageResult<VehicleResponse> list(UUID customerId, PageParameters paging);

    @Operation(summary = "List the authenticated customer's vehicles")
    PageResult<VehicleResponse> listMine(PageParameters paging);

    @Operation(summary = "Update a vehicle")
    VehicleResponse update(UUID vehicleId, UpdateVehicleRequest request);

    @Operation(summary = "Logically remove a vehicle")
    @ApiResponse(responseCode = "204", description = "Removed")
    ResponseEntity<Void> remove(UUID vehicleId);
}
