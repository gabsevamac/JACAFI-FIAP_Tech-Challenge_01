package com.jacafi.tech.auth.adapter.in.web.api;

import com.jacafi.tech.auth.adapter.in.web.dto.LoginRequest;
import com.jacafi.tech.auth.adapter.in.web.dto.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "Access-token authentication")
public interface AuthApi {

    @Operation(summary = "Authenticate an account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    LoginResponse login(LoginRequest request);
}
