package com.jacafi.tech.inventory.api.dto;

import com.jacafi.tech.inventory.domain.MaterialType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body of a registration request. A record, which Jackson deserializes natively and on which
 * Jakarta Validation constraints work as component annotations.
 *
 * <p>The number of decimals allowed in {@code unitPrice} is not declared here: the domain owns
 * that rule, refuses a price it cannot charge exactly, and the error handler turns the refusal
 * into a 400 all the same.
 */
public record RegisterMaterialRequest(
        @Schema(description = "How the workshop calls this material", example = "Filtro de óleo")
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Schema(description = "PART for a component that stays in the vehicle, SUPPLY for material "
                + "consumed during the work", example = "PART")
        @NotNull(message = "type is required")
        MaterialType type,

        @Schema(description = "Price charged per unit, at most two decimal places", example = "49.90")
        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", message = "unitPrice must be zero or positive")
        BigDecimal unitPrice,

        @Schema(description = "Opening balance: units already on the shelf. Omit for a material "
                + "the workshop has yet to buy.", example = "12")
        @PositiveOrZero(message = "initialStock must be zero or positive")
        int initialStock) {
}
