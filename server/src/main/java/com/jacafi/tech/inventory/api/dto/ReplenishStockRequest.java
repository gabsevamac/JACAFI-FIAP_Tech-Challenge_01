package com.jacafi.tech.inventory.api.dto;

import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of a replenishment: how many units arrived. */
public record ReplenishStockRequest(
        @Schema(description = "Units added to the shelf", example = "24")
        @Min(value = 1, message = "quantity must be at least 1") int quantity) {}
