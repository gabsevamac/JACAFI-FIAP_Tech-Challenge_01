package com.jacafi.tech.serviceorder.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ServiceOrderTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
    private static final String ACTOR = "advisor";

    @Test
    void opensReceivedOrderWithRequestedLineSnapshots() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Engine noise",
                List.of(LaborLineItem.of(
                        UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1)),
                List.of(MaterialLineItem.of(
                        UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("39.90"), 2)),
                ACTOR,
                CLOCK);

        assertThat(order.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(order.laborLines()).hasSize(1);
        assertThat(order.materialLines()).hasSize(1);
    }

    @Test
    void calculatesAnEstimateFromFrozenServiceAndMaterialLines() {
        ServiceOrder order = diagnosedOrder();
        order.addLaborLine(
                LaborLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        order.addMaterialLine(
                MaterialLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("20.00"), 3));

        Estimate estimate = order.generateEstimate(ACTOR, CLOCK);

        assertThat(estimate.totalAmount()).isEqualByComparingTo("149.90");
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(order.statusHistory())
                .extracting(StatusChange::status)
                .containsExactly(
                        ServiceOrderStatus.RECEIVED,
                        ServiceOrderStatus.UNDER_DIAGNOSIS,
                        ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void rejectsStatusTransitionsThatBypassTheApprovalGate() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", ACTOR, CLOCK);

        assertThatThrownBy(() -> order.complete(ACTOR, CLOCK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> order.deliver(ACTOR, CLOCK)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void repeatsTheSameApprovalButRefusesItsKeyForARejection() {
        ServiceOrder order = diagnosedOrder();
        order.addLaborLine(
                LaborLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        Estimate pending = order.generateEstimate(ACTOR, CLOCK);
        String key = "external-approval-1";

        Estimate approved = order.approveEstimate(pending.id(), key, ACTOR, CLOCK);

        assertThat(order.approveEstimate(pending.id(), key, ACTOR, CLOCK)).isSameAs(approved);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(order.approvals()).hasSize(1);
        assertThatThrownBy(() -> order.rejectEstimate(pending.id(), key, ACTOR, CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectionReturnsToDiagnosisAndAllowsANewEstimate() {
        ServiceOrder order = diagnosedOrder();
        order.addLaborLine(
                LaborLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        Estimate rejected = order.generateEstimate(ACTOR, CLOCK);

        order.rejectEstimate(rejected.id(), "external-rejection-1", ACTOR, CLOCK);
        order.addMaterialLine(
                MaterialLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("20.00"), 1));
        Estimate next = order.generateEstimate(ACTOR, CLOCK);

        assertThat(rejected.status()).isEqualTo(EstimateStatus.REJECTED);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(next.id()).isNotEqualTo(rejected.id());
        assertThat(next.totalAmount()).isEqualByComparingTo("109.90");
    }

    private static ServiceOrder diagnosedOrder() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", ACTOR, CLOCK);
        order.startDiagnosis(ACTOR, CLOCK);
        return order;
    }
}
