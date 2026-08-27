package com.jacafi.tech.shared.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jacafi.tech.shared.application.AuditEvent;

@ExtendWith(MockitoExtension.class)
class JpaAuditTrailTest {

    @Mock
    private AuditTrailJpaRepository repository;

    @Test
    void mapsTheAppendOnlyAuditEventToTheV07Columns() {
        UUID aggregateId = UUID.randomUUID();
        new JpaAuditTrail(repository)
                .record(new AuditEvent("Vehicle", aggregateId, "UPDATED", "advisor", Instant.EPOCH));

        ArgumentCaptor<AuditTrailJpaEntity> entry = ArgumentCaptor.forClass(AuditTrailJpaEntity.class);
        verify(repository).save(entry.capture());
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "aggregateType"))
                .isEqualTo("Vehicle");
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "aggregateId"))
                .isEqualTo(aggregateId);
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "action")).isEqualTo("UPDATED");
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "actor")).isEqualTo("advisor");
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "occurredAt")).isEqualTo(Instant.EPOCH);
    }
}
