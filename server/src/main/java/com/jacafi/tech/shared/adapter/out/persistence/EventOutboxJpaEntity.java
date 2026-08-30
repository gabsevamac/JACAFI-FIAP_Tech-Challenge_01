package com.jacafi.tech.shared.adapter.out.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event_outbox")
public class EventOutboxJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 60)
    private OutboxEventType eventType;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 60)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EventOutboxJpaEntity() {}

    EventOutboxJpaEntity(
            OutboxEventType eventType,
            String aggregateType,
            UUID aggregateId,
            Map<String, Object> payload,
            Instant createdAt) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = Map.copyOf(payload);
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public OutboxEventType eventType() {
        return eventType;
    }

    public String aggregateType() {
        return aggregateType;
    }

    public UUID aggregateId() {
        return aggregateId;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public void markProcessed(Instant processedAt) {
        status = OutboxEventStatus.PROCESSED;
        this.processedAt = processedAt;
        lastError = null;
    }

    public void markFailed(String message) {
        attempts++;
        if (attempts >= 5) {
            status = OutboxEventStatus.FAILED;
        }
        lastError = message == null ? "Processing failed" : message.substring(0, Math.min(500, message.length()));
    }
}
