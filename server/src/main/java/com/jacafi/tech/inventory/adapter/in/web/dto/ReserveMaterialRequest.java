package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveMaterialRequest(
        @NotNull UUID serviceOrderId, @Positive int quantity) {}
