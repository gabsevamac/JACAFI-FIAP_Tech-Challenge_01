package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.jacafi.tech.inventory.domain.entity.MaterialType;

public record RegisterMaterialRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull MaterialType type,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @PositiveOrZero int initialStock) {}
