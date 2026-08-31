package com.jacafi.tech.serviceorder.application.port;

import java.util.UUID;

import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

public interface StatusNotificationPort {

    void notifyStatusChanged(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status);
}
