package com.jacafi.tech.vehicle.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterVehicleRequest(
        @NotBlank @Size(max = 20) String licensePlate,
        @NotBlank @Size(max = 60) String make,
        @NotBlank @Size(max = 60) String model,
        @Min(1886) @Max(9999) int modelYear,
        @NotNull UUID customerId) {

    @Override
    public String toString() {
        return "RegisterVehicleRequest[licensePlate=***, customerIdPresent=%s]".formatted(customerId != null);
    }
}
