package com.jacafi.tech.vehicle.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.vehicle.api.dto.RegisterVehicleRequest;
import com.jacafi.tech.vehicle.api.dto.UpdateVehicleRequest;
import com.jacafi.tech.vehicle.api.dto.VehicleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Documented contract of the vehicle endpoints.
 *
 * <p>The OpenAPI annotations live here rather than on the controller, and the split is by purpose:
 * this file describes the API to whoever consumes it, while the controller decides how a request
 * becomes a call. Mixed together, the documentation outweighed the code it described and every
 * change to either meant reading past the other.
 *
 * <p>Only the description belongs here. Spring's own annotations — the mapping, the path variables,
 * the request body, the validation trigger — stay on the implementation, where the framework
 * expects them and where they behave predictably.
 *
 * <p>Not a port and not an abstraction: it has exactly one implementation and will never have
 * another. It is a place to put annotations.
 */
@Tag(name = "Vehicles", description = "Vehicle registry")
@SecurityRequirement(name = "bearer-jwt")
public interface VehicleApi {

    @Operation(
            summary = "Register a vehicle",
            description = "The license plate is normalized before validation, so abc-1234 and "
                    + "ABC1234 are the same plate. A plate already held by an active vehicle is "
                    + "rejected with 409.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registered"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid plate format or attribute out of range",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "License plate already registered to an active vehicle",
                content = @Content)
    })
    ResponseEntity<VehicleResponse> register(RegisterVehicleRequest request, Authentication authentication);

    @Operation(summary = "Find a vehicle by identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
        @ApiResponse(responseCode = "404", description = "No active vehicle with this identifier", content = @Content)
    })
    VehicleResponse findById(@Parameter(description = "Identifier assigned at registration") UUID id);

    @Operation(
            summary = "Find by license plate, or list a customer's vehicles",
            description = "Exactly one of licensePlate or customerId must be given. A plate "
                    + "identifies a single vehicle and yields one object; customerId yields a page. "
                    + "Sortable fields: registeredAt, make, model, modelYear. Any other value is "
                    + "rejected with 400.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(
                responseCode = "400",
                description = "Neither or both query parameters were given",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "No active vehicle with this license plate",
                content = @Content)
    })
    ResponseEntity<?> findByQuery(
            @Parameter(description = "Exact plate, in either layout; separators are ignored") String licensePlate,
            @Parameter(description = "Owner of the vehicles to list") UUID customerId,
            PageParameters paging);

    @Operation(
            summary = "Correct make, model and model year",
            description = "The license plate cannot be changed: it is the vehicle's business "
                    + "identity. Correcting a plate recorded wrongly is a separate use case.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Attribute missing or out of range", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
        @ApiResponse(responseCode = "404", description = "No active vehicle with this identifier", content = @Content)
    })
    VehicleResponse update(
            @Parameter(description = "Identifier of the vehicle to correct") UUID id,
            UpdateVehicleRequest request,
            Authentication authentication);

    @Operation(
            summary = "Remove a vehicle from the active registry",
            description = "The row is not deleted. The plate is replaced by an irreversible token "
                    + "and the record survives with its service history, which is required by law "
                    + "and by warranty (LGPD Art. 16 I), while the right to erasure is satisfied "
                    + "by the plate being gone (Art. 18 VI). The plate becomes available for a new "
                    + "registration.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
        @ApiResponse(responseCode = "404", description = "No active vehicle with this identifier", content = @Content)
    })
    ResponseEntity<Void> remove(
            @Parameter(description = "Identifier of the vehicle to remove") UUID id, Authentication authentication);
}
