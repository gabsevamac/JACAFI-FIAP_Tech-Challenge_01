package com.jacafi.tech.serviceorder.adapter.out.notification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.jacafi.tech.customer.application.port.CustomerRepository;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;

@Component
public class ResendStatusEmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResendStatusEmailSender.class);

    private final CustomerRepository customers;
    private final RestClient client;
    private final boolean enabled;
    private final String apiKey;
    private final String from;

    ResendStatusEmailSender(
            CustomerRepository customers,
            @Value("${resend.enabled:false}") boolean enabled,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String from) {
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.client = RestClient.builder().baseUrl("https://api.resend.com").build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.from = from;
    }

    public void send(UUID outboxEventId, UUID customerId, UUID serviceOrderId, ServiceOrderStatus status) {
        if (!enabled) {
            LOGGER.info(
                    "Service order status e-mail skipped because Resend is disabled: serviceOrderId={}",
                    serviceOrderId);
            return;
        }

        Customer customer = customers.findById(customerId).orElseThrow();
        if (!customer.active()) {
            return;
        }
        if (apiKey.isBlank() || from.isBlank()) {
            throw new IllegalStateException("Resend credentials must be configured when e-mail delivery is enabled");
        }

        client.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", "service-order-status/" + outboxEventId)
                .body(Map.of(
                        "from",
                        from,
                        "to",
                        List.of(customer.email()),
                        "subject",
                        "Service order update " + serviceOrderId,
                        "text",
                        "Your service order " + serviceOrderId + " is now in status " + status + "."))
                .retrieve()
                .toBodilessEntity();
    }
}
