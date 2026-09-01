package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServiceOrderStatusResponse(UUID serviceOrderId, ServiceOrderStatus status,
                                         List<EstimateResponse> estimates, Instant updatedAt) {
    public static ServiceOrderStatusResponse from(ServiceOrder order) {
        var estimates = order.estimates().stream().map(EstimateResponse::from).toList();
        return new ServiceOrderStatusResponse(order.id(), order.status(), estimates, order.updatedAt());
    }
}
