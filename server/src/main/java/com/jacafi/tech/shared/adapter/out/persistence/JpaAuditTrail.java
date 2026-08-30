package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

@Component
public class JpaAuditTrail implements AuditTrailPort {

    private final EventOutboxPublisher publisher;

    JpaAuditTrail(EventOutboxPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        publisher.publishAudit(event);
    }
}
