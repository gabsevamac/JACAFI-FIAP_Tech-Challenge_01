package com.jacafi.tech.vehicle.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of an update request.
 *
 * <p>No license plate: it is the vehicle's business identity and immutable after registration.
 * Correcting a plate recorded wrongly is a separate use case, not a field here.
 */
public record UpdateVehicleRequest(
        @Schema(example = "Chevrolet")
        @NotBlank(message = "make is required")
        String make,

        @Schema(example = "Onix")
        @NotBlank(message = "model is required")
        String model,

        @Schema(description = "Model year, from 1900 up to next year", example = "2021")
        @Min(value = 1900, message = "modelYear must not be earlier than 1900")
        int modelYear) {
}
