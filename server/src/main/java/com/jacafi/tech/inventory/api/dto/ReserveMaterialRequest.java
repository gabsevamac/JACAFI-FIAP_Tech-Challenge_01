package com.jacafi.tech.inventory.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of a reservation: which order is holding the material, and how much of it. */
public record ReserveMaterialRequest(
        @Schema(description = "Service order whose approved estimate authorizes the material")
        @NotNull(message = "serviceOrderId is required") UUID serviceOrderId,

        @Schema(description = "Units to hold for that order", example = "2")
        @Min(value = 1, message = "quantity must be at least 1") int quantity) {}
