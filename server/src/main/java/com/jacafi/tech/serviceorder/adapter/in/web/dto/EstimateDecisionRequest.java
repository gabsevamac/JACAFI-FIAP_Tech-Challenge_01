package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.jacafi.tech.serviceorder.domain.entity.EstimateDecision;

public record EstimateDecisionRequest(
        @NotNull EstimateDecision decision,
        @NotBlank @Size(max = 120) String idempotencyKey) {}
