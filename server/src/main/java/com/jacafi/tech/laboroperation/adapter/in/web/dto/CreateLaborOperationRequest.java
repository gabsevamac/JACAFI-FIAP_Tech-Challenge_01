package com.jacafi.tech.laboroperation.adapter.in.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLaborOperationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice) {}
