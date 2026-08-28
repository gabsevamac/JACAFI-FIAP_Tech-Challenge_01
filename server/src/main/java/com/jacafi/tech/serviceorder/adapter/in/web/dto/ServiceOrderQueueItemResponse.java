package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

public record ServiceOrderQueueItemResponse(
        UUID serviceOrderId, UUID customerId, UUID vehicleId, ServiceOrderStatus status, Instant createdAt) {
    public static ServiceOrderQueueItemResponse from(ServiceOrder order) {
        return new ServiceOrderQueueItemResponse(
                order.id(), order.customerId(), order.vehicleId(), order.status(), order.createdAt());
    }
}
