package com.jacafi.tech.shared.adapter.out.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.jacafi.tech.shared.application.AuditEvent;

/** Storage shape of one append-only audit event. */
@Entity
@Table(name = "audit_trail")
public class AuditTrailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "outbox_event_id", nullable = false, updatable = false, unique = true)
    private UUID outboxEventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 60)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private String action;

    @Column(name = "actor", nullable = false, updatable = false, length = 120)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, String> beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, String> afterState;

    protected AuditTrailJpaEntity() {}

    public AuditTrailJpaEntity(UUID outboxEventId, AuditEvent event) {
        this.outboxEventId = outboxEventId;
        this.aggregateType = event.aggregateType();
        this.aggregateId = event.aggregateId();
        this.action = event.action();
        this.actor = event.actor();
        this.occurredAt = event.occurredAt();
        this.beforeState = event.beforeState();
        this.afterState = event.afterState();
    }

    @Override
    public String toString() {
        return "AuditTrailJpaEntity[id=" + id
                + ", aggregateType=" + aggregateType
                + ", aggregateId=" + aggregateId
                + ", action=" + action
                + "]";
    }
}
