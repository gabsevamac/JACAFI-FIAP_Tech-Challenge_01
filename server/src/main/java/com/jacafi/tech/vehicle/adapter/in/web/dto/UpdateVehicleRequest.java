package com.jacafi.tech.vehicle.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVehicleRequest(
        @NotBlank @Size(max = 60) String make,
        @NotBlank @Size(max = 60) String model,
        @Min(1886) @Max(9999) int modelYear) {

    @Override
    public String toString() {
        return "UpdateVehicleRequest[make=***, model=***, modelYear=%d]".formatted(modelYear);
    }
}
