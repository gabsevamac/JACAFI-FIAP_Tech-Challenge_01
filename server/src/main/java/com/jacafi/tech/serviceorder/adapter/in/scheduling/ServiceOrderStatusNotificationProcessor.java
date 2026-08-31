package com.jacafi.tech.serviceorder.adapter.in.scheduling;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.serviceorder.adapter.out.notification.ResendStatusEmailSender;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxJpaEntity;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxJpaRepository;
import com.jacafi.tech.shared.adapter.out.persistence.OutboxEventStatus;
import com.jacafi.tech.shared.adapter.out.persistence.OutboxEventType;

@Component
public class ServiceOrderStatusNotificationProcessor {
    private final EventOutboxJpaRepository outbox;
    private final ResendStatusEmailSender sender;
    private final Clock clock;

    ServiceOrderStatusNotificationProcessor(
            EventOutboxJpaRepository outbox, ResendStatusEmailSender sender, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${outbox.processing-delay-ms:5000}")
    @Transactional
    public void process() {
        outbox.findTop20ByEventTypeAndStatusOrderByCreatedAtAsc(
                        OutboxEventType.SERVICE_ORDER_STATUS_NOTIFICATION, OutboxEventStatus.PENDING)
                .forEach(this::notifyCustomer);
    }

    private void notifyCustomer(EventOutboxJpaEntity event) {
        try {
            sender.send(
                    event.id(),
                    UUID.fromString((String) event.payload().get("customerId")),
                    event.aggregateId(),
                    ServiceOrderStatus.valueOf((String) event.payload().get("status")));
            event.markProcessed(clock.instant());
        } catch (RuntimeException exception) {
            event.markFailed(exception.getClass().getSimpleName());
        }
    }
}
