package com.jacafi.tech.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jacafi.tech.shared.application.AuditEvent;

@ExtendWith(MockitoExtension.class)
class JpaAuditTrailTest {

    @Mock
    private EventOutboxJpaRepository repository;

    @Test
    void queuesTheAuditEventInTheTransactionalOutbox() {
        UUID aggregateId = UUID.randomUUID();
        EventOutboxPublisher publisher =
                new EventOutboxPublisher(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        new JpaAuditTrail(publisher)
                .record(new AuditEvent("Vehicle", aggregateId, "UPDATED", "advisor", Instant.EPOCH));

        ArgumentCaptor<EventOutboxJpaEntity> entry = ArgumentCaptor.forClass(EventOutboxJpaEntity.class);
        verify(repository).save(entry.capture());
        assertThat(entry.getValue().aggregateType()).isEqualTo("Vehicle");
        assertThat(entry.getValue().aggregateId()).isEqualTo(aggregateId);
        assertThat(entry.getValue().eventType()).isEqualTo(OutboxEventType.AUDIT);
        assertThat(entry.getValue().payload())
                .containsEntry("action", "UPDATED")
                .containsEntry("actor", "advisor")
                .containsEntry("occurredAt", Instant.EPOCH.toString());
    }
}
