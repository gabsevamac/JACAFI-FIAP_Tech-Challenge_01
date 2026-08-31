package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

public record UpdateServiceOrderStatusRequest(@NotNull ServiceOrderStatus status) {}
