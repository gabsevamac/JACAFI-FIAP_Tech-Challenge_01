package com.jacafi.tech.shared.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Storage shape of one append-only audit event. */
@Entity
@Table(name = "audit_trail")
public class AuditTrailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

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

    protected AuditTrailJpaEntity() {}

    AuditTrailJpaEntity(String aggregateType, UUID aggregateId, String action, String actor, Instant occurredAt) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.action = action;
        this.actor = actor;
        this.occurredAt = occurredAt;
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
