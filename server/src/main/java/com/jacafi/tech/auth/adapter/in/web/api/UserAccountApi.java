package com.jacafi.tech.auth.adapter.in.web.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.auth.adapter.in.web.dto.CreateUserAccountRequest;
import com.jacafi.tech.auth.adapter.in.web.dto.UserAccountResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User accounts", description = "Account administration and current account")
@SecurityRequirement(name = "bearer-jwt")
public interface UserAccountApi {

    @Operation(summary = "Create an account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid account", content = @Content),
        @ApiResponse(responseCode = "403", description = "Administrator role required", content = @Content),
        @ApiResponse(responseCode = "409", description = "Username already exists", content = @Content)
    })
    ResponseEntity<UserAccountResponse> create(CreateUserAccountRequest request);

    @Operation(summary = "List all accounts")
    List<UserAccountResponse> list();

    @Operation(summary = "Read an account")
    UserAccountResponse get(UUID id);

    @Operation(summary = "Read the authenticated account")
    UserAccountResponse me();

    @Operation(summary = "Deactivate an account")
    ResponseEntity<Void> deactivate(UUID id);
}
