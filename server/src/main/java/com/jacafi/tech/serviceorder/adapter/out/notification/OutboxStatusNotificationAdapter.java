package com.jacafi.tech.serviceorder.adapter.out.notification;

import java.util.Objects;
import java.util.UUID;

import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxPublisher;

public class OutboxStatusNotificationAdapter implements StatusNotificationPort {
    private final EventOutboxPublisher publisher;

    public OutboxStatusNotificationAdapter(EventOutboxPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void notifyStatusChanged(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status) {
        publisher.publishServiceOrderStatusNotification(serviceOrderId, customerId, status);
    }
}
