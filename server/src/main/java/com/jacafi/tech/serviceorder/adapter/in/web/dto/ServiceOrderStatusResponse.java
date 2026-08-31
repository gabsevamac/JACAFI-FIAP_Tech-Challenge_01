package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

public record ServiceOrderStatusResponse(UUID serviceOrderId, ServiceOrderStatus status, Instant updatedAt) {
    public static ServiceOrderStatusResponse from(ServiceOrder order) {
        return new ServiceOrderStatusResponse(order.id(), order.status(), order.updatedAt());
    }
}
