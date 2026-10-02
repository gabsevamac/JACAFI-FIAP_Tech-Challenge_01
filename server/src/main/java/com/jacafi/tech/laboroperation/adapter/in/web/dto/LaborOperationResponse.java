package com.jacafi.tech.laboroperation.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;

public record LaborOperationResponse(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {
    public static LaborOperationResponse from(LaborOperation operation) {
        return new LaborOperationResponse(
                operation.id(),
                operation.name(),
                operation.description(),
                operation.basePrice(),
                operation.active(),
                operation.createdAt(),
                operation.updatedAt(),
                operation.version());
    }
}
