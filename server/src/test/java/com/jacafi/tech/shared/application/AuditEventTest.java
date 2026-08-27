package com.jacafi.tech.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
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
}
