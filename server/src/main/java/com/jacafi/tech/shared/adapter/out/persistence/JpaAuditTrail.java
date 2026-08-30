package com.jacafi.tech.shared.adapter.out.persistence;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

/** JPA adapter for {@link AuditTrailPort}. */
@Component
public class JpaAuditTrail implements AuditTrailPort {

    private final EventOutboxPublisher publisher;

    JpaAuditTrail(EventOutboxPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    /**
     * Joins the caller's transaction, and does so deliberately.
     *
     * <p>{@code REQUIRES_NEW} would keep the trail entry when the business write rolls back, which
     * sounds like the safer choice and is not: it would record a change that never happened, and a
     * trail that reports non-events is worse than one with gaps, because there is no way to tell
     * which entries are real.
     *
     * <p>The converse — a business write committing while its trail entry is lost — cannot happen
     * either, since both share the transaction.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        publisher.publishAudit(event);
    }
}
