package com.jacafi.tech.inventory.adapter.in.web.dto;

import jakarta.validation.constraints.Positive;

public record ReplenishStockRequest(@Positive int quantity) {}
