package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RequestedLaborOperationRequest(
        @NotNull UUID laborOperationId, @Min(1) int quantity) {}
