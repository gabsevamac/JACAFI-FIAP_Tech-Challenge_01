package com.jacafi.tech.shared.infrastructure.persistence;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.FieldChange;

/** JPA adapter for {@link AuditTrailPort}. */
@Component
public class JpaAuditTrail implements AuditTrailPort {

    private final AuditTrailJpaRepository repository;

    JpaAuditTrail(AuditTrailJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
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
    public void record(FieldChange change) {
        Objects.requireNonNull(change, "change must not be null");

        repository.save(new AuditTrailJpaEntity(
                change.aggregateType(),
                change.aggregateId(),
                change.fieldName(),
                change.oldValue(),
                change.newValue(),
                change.reason(),
                change.changedAt(),
                change.changedBy()));
    }
}
