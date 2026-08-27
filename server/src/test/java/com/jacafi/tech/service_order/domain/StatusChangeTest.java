package com.jacafi.tech.service_order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusChangeTest {

    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

    @Test
    @DisplayName("creates initial status change")
    void createsInitialStatusChange() {
        var change = StatusChange.initial(ServiceOrderStatus.RECEIVED, NOW, "advisor-1");

        assertThat(change.fromStatus()).isNull();
        assertThat(change.toStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(change.occurredAt()).isEqualTo(NOW);
        assertThat(change.actor()).isEqualTo("advisor-1");
    }

    @Test
    @DisplayName("creates status transition change")
    void createsStatusTransition() {
        var change = StatusChange.transition(
                ServiceOrderStatus.RECEIVED, ServiceOrderStatus.UNDER_DIAGNOSIS, NOW, "mechanic-1");

        assertThat(change.fromStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(change.toStatus()).isEqualTo(ServiceOrderStatus.UNDER_DIAGNOSIS);
        assertThat(change.occurredAt()).isEqualTo(NOW);
        assertThat(change.actor()).isEqualTo("mechanic-1");
    }

    @Test
    @DisplayName("defaults null actor to system")
    void defaultsActorToSystem() {
        var change = StatusChange.initial(ServiceOrderStatus.RECEIVED, NOW, null);
        assertThat(change.actor()).isEqualTo("system");

        var transition =
                StatusChange.transition(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.UNDER_DIAGNOSIS, NOW, null);
        assertThat(transition.actor()).isEqualTo("system");
    }

    @Test
    @DisplayName("rejects null required arguments")
    void rejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> StatusChange.initial(null, NOW, "actor"));

        assertThatNullPointerException()
                .isThrownBy(() -> StatusChange.initial(ServiceOrderStatus.RECEIVED, null, "actor"));

        assertThatNullPointerException()
                .isThrownBy(() -> StatusChange.transition(null, ServiceOrderStatus.UNDER_DIAGNOSIS, NOW, "actor"));
    }
}
