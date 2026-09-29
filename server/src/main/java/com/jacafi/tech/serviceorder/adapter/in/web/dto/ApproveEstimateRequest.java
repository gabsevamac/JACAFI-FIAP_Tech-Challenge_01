package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveEstimateRequest(
        @NotBlank @Size(max = 120) String idempotencyKey) {}
