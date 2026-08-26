package com.jacafi.tech.shared.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * Storage shape of one audit trail entry.
 *
 * <p>Deliberately does <em>not</em> extend {@code AuditableEntity}. The trail has no lifecycle to
 * audit: an entry is written once, never modified, never removed. Inheriting {@code updatedBy},
 * {@code deletedAt} and {@code version} would furnish it with exactly the operations it exists to
 * make impossible.
 */
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

    @Column(name = "field_name", nullable = false, updatable = false, length = 60)
    private String fieldName;

    @PersonalData("LGPD Art. 5 I — holds plates and taxpayer registrations, retained under Art. 16 I")
    @Column(name = "old_value", updatable = false)
    private String oldValue;

    @PersonalData("LGPD Art. 5 I — holds plates and taxpayer registrations, retained under Art. 16 I")
    @Column(name = "new_value", updatable = false)
    private String newValue;

    @Column(name = "reason", updatable = false, length = 200)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, updatable = false, length = 120)
    private String changedBy;

    protected AuditTrailJpaEntity() {}

    AuditTrailJpaEntity(
            String aggregateType,
            UUID aggregateId,
            String fieldName,
            String oldValue,
            String newValue,
            String reason,
            Instant changedAt,
            String changedBy) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    /**
     * Masked, and without the values. The entry's whole purpose is to hold personal data intact,
     * so its {@code toString} is the single most likely way for that data to reach a log by
     * accident — an exception message, a debug statement, a collection interpolated into a string.
     */
    @Override
    public String toString() {
        return "AuditTrailJpaEntity[id=" + id
                + ", aggregateType=" + aggregateType
                + ", aggregateId=" + aggregateId
                + ", fieldName=" + fieldName
                + ", values=<redacted>]";
    }
}
