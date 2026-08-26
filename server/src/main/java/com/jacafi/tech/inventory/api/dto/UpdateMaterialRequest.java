package com.jacafi.tech.inventory.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of a correction request.
 *
 * <p>No type and no quantity, and their absence is the rule enforced by the type system rather
 * than by a check: the material type is immutable after registration, and stock moves only through
 * the commands that say what moved it.
 */
public record UpdateMaterialRequest(
        @Schema(example = "Filtro de óleo")
        @NotBlank(message = "name is required") @Size(max = 120, message = "name must be at most 120 characters") String name,

        @Schema(
                description = "Price charged per unit from now on. Orders already priced keep the "
                        + "price frozen at the moment their items were launched.",
                example = "54.90")
        @NotNull(message = "unitPrice is required") @DecimalMin(value = "0.00", message = "unitPrice must be zero or positive") BigDecimal unitPrice) {}
