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
                List.of(ServiceLineItem.of(
                        UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1)),
                List.of(MaterialLineItem.of(
                        UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("39.90"), 2)),
                ACTOR,
                CLOCK);

        assertThat(order.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(order.serviceLines()).hasSize(1);
        assertThat(order.materialLines()).hasSize(1);
    }

    @Test
    void calculatesAnEstimateFromFrozenServiceAndMaterialLines() {
        ServiceOrder order = diagnosedOrder();
        order.addServiceLine(
                ServiceLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        order.addMaterialLine(
                MaterialLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("20.00"), 3));

        Estimate estimate = order.generateEstimate(ACTOR, CLOCK);

        assertThat(estimate.totalAmount()).isEqualByComparingTo("149.90");
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(order.statusHistory())
                .extracting(StatusHistory::status)
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
    void repeatsTheSameEstimateDecisionButRejectsAConflictingReplay() {
        ServiceOrder order = diagnosedOrder();
        order.addServiceLine(
                ServiceLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        Estimate pending = order.generateEstimate(ACTOR, CLOCK);
        String key = "external-decision-1";

        Estimate approved = order.decideEstimate(pending.id(), EstimateDecision.APPROVE, key, ACTOR, CLOCK);

        assertThat(order.decideEstimate(pending.id(), EstimateDecision.APPROVE, key, ACTOR, CLOCK))
                .isSameAs(approved);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> order.decideEstimate(pending.id(), EstimateDecision.REJECT, key, ACTOR, CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectionReturnsToDiagnosisAndAllowsASupplementaryEstimate() {
        ServiceOrder order = diagnosedOrder();
        order.addServiceLine(
                ServiceLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        Estimate rejected = order.generateEstimate(ACTOR, CLOCK);

        order.decideEstimate(rejected.id(), EstimateDecision.REJECT, "external-decision-2", ACTOR, CLOCK);
        order.addMaterialLine(
                MaterialLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Engine oil", new BigDecimal("20.00"), 1));
        Estimate supplementary = order.generateEstimate(ACTOR, CLOCK);

        assertThat(rejected.status()).isEqualTo(EstimateStatus.REJECTED);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(supplementary.id()).isNotEqualTo(rejected.id());
        assertThat(supplementary.totalAmount()).isEqualByComparingTo("109.90");
    }

    private static ServiceOrder diagnosedOrder() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", ACTOR, CLOCK);
        order.startDiagnosis(ACTOR, CLOCK);
        return order;
    }
}
