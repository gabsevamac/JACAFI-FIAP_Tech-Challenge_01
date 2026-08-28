package com.jacafi.tech.serviceorder.adapter.out.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

public class LoggingStatusNotificationAdapter implements StatusNotificationPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStatusNotificationAdapter.class);

    @Override
    public void notifyStatusChanged(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status) {
        LOGGER.info("Status notification requested for serviceOrderId={} status={}", serviceOrderId, status);
    }
}
