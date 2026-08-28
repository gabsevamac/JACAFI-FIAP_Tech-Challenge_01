package com.jacafi.tech.serviceorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.EstimateDecision;
import com.jacafi.tech.serviceorder.domain.entity.ServiceLineItem;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

class DecideEstimateServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void persistsTheApprovedDecisionBeforeRecordingTheAuditEvent() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", "advisor", CLOCK);
        order.startDiagnosis("advisor", CLOCK);
        order.addServiceLine(
                ServiceLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        Estimate pending = order.generateEstimate("advisor", CLOCK);
        Orders orders = new Orders(order);
        Trail trail = new Trail();
        DecideEstimateService service = new DecideEstimateService(orders, trail, operational(), CLOCK);

        Estimate approved = service.decide(order.id(), pending.id(), EstimateDecision.APPROVE, "notification-1");

        assertThat(approved.status().name()).isEqualTo("APPROVED");
        assertThat(orders.saved).isSameAs(order);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("ESTIMATE_APPROVED");
    }

    private static ServiceOrderAccessPolicy operational() {
        CurrentAuthenticatedUserPort user =
                () -> new AuthenticatedUser(UUID.randomUUID(), "advisor", Set.of(Role.SERVICE_ADVISOR), null);
        return new ServiceOrderAccessPolicy(user);
    }

    private static final class Orders implements ServiceOrderRepositoryPort {
        private final ServiceOrder order;
        private ServiceOrder saved;

        private Orders(ServiceOrder order) {
            this.order = order;
        }

        @Override
        public ServiceOrder save(ServiceOrder serviceOrder) {
            saved = serviceOrder;
            return serviceOrder;
        }

        @Override
        public Optional<ServiceOrder> findById(UUID id) {
            return order.id().equals(id) ? Optional.of(order) : Optional.empty();
        }
    }

    private static final class Trail implements AuditTrailPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }
}
