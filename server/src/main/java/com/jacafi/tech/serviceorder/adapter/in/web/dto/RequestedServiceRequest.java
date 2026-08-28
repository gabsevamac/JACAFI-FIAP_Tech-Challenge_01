package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RequestedServiceRequest(
        @NotNull UUID serviceCatalogItemId, @Min(1) int quantity) {}
