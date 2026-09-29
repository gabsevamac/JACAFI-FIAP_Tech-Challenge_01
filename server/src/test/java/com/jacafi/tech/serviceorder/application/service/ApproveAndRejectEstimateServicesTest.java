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
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.Approval;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.LaborLineItem;
import com.jacafi.tech.serviceorder.domain.entity.Rejection;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

class ApproveAndRejectEstimateServicesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void persistsTheApprovalBeforeRecordingTheAuditEvent() {
        ServiceOrder order = orderAwaitingApproval();
        Estimate pending = order.estimates().getFirst();
        Orders orders = new Orders(order);
        Trail trail = new Trail();
        ApproveEstimateService service =
                new ApproveEstimateService(orders, notifications(), trail, operational(), CLOCK);

        Estimate approved = service.approve(order.id(), pending.id(), "notification-1");

        assertThat(approved.status().name()).isEqualTo("APPROVED");
        assertThat(orders.saved).isSameAs(order);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(order.approvals())
                .singleElement()
                .extracting(Approval::estimateId)
                .isEqualTo(pending.id());
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("ESTIMATE_APPROVED");
    }

    @Test
    void persistsTheRejectionBeforeRecordingTheAuditEvent() {
        ServiceOrder order = orderAwaitingApproval();
        Estimate pending = order.estimates().getFirst();
        Orders orders = new Orders(order);
        Trail trail = new Trail();
        RejectEstimateService service = new RejectEstimateService(orders, notifications(), trail, operational(), CLOCK);

        Estimate rejected = service.reject(order.id(), pending.id(), "notification-2");

        assertThat(rejected.status().name()).isEqualTo("REJECTED");
        assertThat(orders.saved).isSameAs(order);
        assertThat(order.status()).isEqualTo(ServiceOrderStatus.UNDER_DIAGNOSIS);
        assertThat(order.rejections())
                .singleElement()
                .extracting(Rejection::estimateId)
                .isEqualTo(pending.id());
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("ESTIMATE_REJECTED");
    }

    private static ServiceOrder orderAwaitingApproval() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", "advisor", CLOCK);
        order.startDiagnosis("advisor", CLOCK);
        order.addLaborLine(
                LaborLineItem.of(UUID.randomUUID(), UUID.randomUUID(), "Oil change", new BigDecimal("89.90"), 1));
        order.generateEstimate("advisor", CLOCK);
        return order;
    }

    private static ServiceOrderAccessPolicy operational() {
        CurrentAuthenticatedUserPort user =
                () -> new AuthenticatedUser(UUID.randomUUID(), "advisor", Set.of(Role.SERVICE_ADVISOR), null);
        return new ServiceOrderAccessPolicy(user);
    }

    private static final class Orders implements ServiceOrderRepository {
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

        @Override
        public PageResult<ServiceOrder> findOperationalQueue(PageQuery query) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Trail implements AuditTrailPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }

    private static StatusNotificationPort notifications() {
        return (serviceOrderId, customerId, status) -> {};
    }
}
