package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.EstimateStatus;

public record EstimateResponse(
        UUID estimateId, EstimateStatus status, BigDecimal totalAmount, Instant createdAt, Instant respondedAt) {
    public static EstimateResponse from(Estimate estimate) {
        return new EstimateResponse(
                estimate.id(), estimate.status(), estimate.totalAmount(), estimate.createdAt(), estimate.respondedAt());
    }
}
