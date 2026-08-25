package com.jacafi.tech.vehicle.api.dto;

import com.jacafi.tech.shared.lgpd.PersonalData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body of a registration request. A record, which Jackson deserializes natively and on which
 * Jakarta Validation constraints work as component annotations.
 *
 * <p>The upper bound of {@code modelYear} is not declared here: it moves with the calendar, and a
 * constant in an annotation cannot. The domain enforces it, and the error handler turns the
 * refusal into a 400 all the same.
 */
public record RegisterVehicleRequest(
        @PersonalData("LGPD Art. 5 I")
        @Schema(description = "Brazilian license plate, old or Mercosur layout. Separators and "
                + "lower case are accepted and normalized.", example = "ABC1D23")
        @NotBlank(message = "licensePlate is required")
        String licensePlate,

        @Schema(description = "Manufacturer", example = "Volkswagen")
        @NotBlank(message = "make is required")
        String make,

        @Schema(example = "Gol")
        @NotBlank(message = "model is required")
        String model,

        @Schema(description = "Model year, from 1900 up to next year", example = "2020")
        @Min(value = 1900, message = "modelYear must not be earlier than 1900")
        int modelYear,

        @Schema(description = "Identifier of the customer responsible for the vehicle")
        @NotNull(message = "customerId is required")
        UUID customerId) {

    /** Never prints the plate: a request body is as loggable as anything else. */
    @Override
    public String toString() {
        return "RegisterVehicleRequest[make=%s, model=%s, modelYear=%d, customerId=%s]"
                .formatted(make, model, modelYear, customerId);
    }
}
