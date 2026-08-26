package com.jacafi.tech.inventory.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of a withdrawal: which order is taking the material it reserved.
 *
 * <p>No quantity. What leaves is what that order reserved, and only that — the reservation is the
 * proof that an approved estimate authorized this material, and a quantity here would be a way to
 * take more than was authorized.
 */
public record WithdrawMaterialRequest(
        @Schema(description = "Service order taking the material it reserved")
        @NotNull(message = "serviceOrderId is required") UUID serviceOrderId) {}
