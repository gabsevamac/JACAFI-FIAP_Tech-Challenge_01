package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record WithdrawMaterialRequest(@NotNull UUID serviceOrderId) {}
