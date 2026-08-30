package com.jacafi.tech.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void holdsOnlyTheSchemaFieldsOfAnAppendOnlyAuditEntry() {
        AuditEvent event = new AuditEvent("Vehicle", UUID.randomUUID(), "UPDATED", "advisor", Instant.EPOCH);

        assertThat(event.action()).isEqualTo("UPDATED");
        assertThat(event.actor()).isEqualTo("advisor");
    }

    @Test
    void rejectsBlankAuditIdentity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AuditEvent("Vehicle", UUID.randomUUID(), "", "advisor", Instant.EPOCH));
    }

    @Test
    void preservesOnlyExplicitBeforeAndAfterState() {
        AuditEvent event = new AuditEvent(
                "ServiceOrder",
                UUID.randomUUID(),
                "STATUS_UPDATED",
                "technician",
                Instant.EPOCH,
                Map.of("status", "IN_PROGRESS"),
                Map.of("status", "COMPLETED"));

        assertThat(event.beforeState()).containsExactlyEntriesOf(Map.of("status", "IN_PROGRESS"));
        assertThat(event.afterState()).containsExactlyEntriesOf(Map.of("status", "COMPLETED"));
    }
}
