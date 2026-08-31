package com.jacafi.tech.serviceorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrderStatus;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

class UpdateServiceOrderStatusServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void completesAnInProgressOrderAndRequestsAStatusNotification() {
        ServiceOrder order = inProgressOrder();
        Orders orders = new Orders(order);
        Trail trail = new Trail();
        Notifications notifications = new Notifications();
        UpdateServiceOrderStatusService service =
                new UpdateServiceOrderStatusService(orders, notifications, trail, technician(), CLOCK);

        service.update(order.id(), ServiceOrderStatus.COMPLETED);

        assertThat(order.status()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(orders.saved).isSameAs(order);
        assertThat(notifications.events)
                .containsExactly(new Notification(order.id(), order.customerId(), ServiceOrderStatus.COMPLETED));
        assertThat(trail.events).singleElement().satisfies(event -> {
            assertThat(event.action()).isEqualTo("STATUS_UPDATED");
            assertThat(event.beforeState()).containsExactlyEntriesOf(Map.of("status", "IN_PROGRESS"));
            assertThat(event.afterState()).containsExactlyEntriesOf(Map.of("status", "COMPLETED"));
        });
    }

    private static ServiceOrder inProgressOrder() {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Engine noise", "advisor", CLOCK);
        order.startDiagnosis("advisor", CLOCK);
        order.generateEstimate("advisor", CLOCK);
        order.decideEstimate(
                order.estimates().getFirst().id(),
                com.jacafi.tech.serviceorder.domain.entity.EstimateDecision.APPROVE,
                "approval-1",
                "advisor",
                CLOCK);
        return order;
    }

    private static ServiceOrderAccessPolicy technician() {
        CurrentAuthenticatedUserPort user =
                () -> new AuthenticatedUser(UUID.randomUUID(), "technician", Set.of(Role.TECHNICIAN), null);
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

    private static final class Notifications implements StatusNotificationPort {
        private final List<Notification> events = new ArrayList<>();

        @Override
        public void notifyStatusChanged(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status) {
            events.add(new Notification(serviceOrderId, customerId, status));
        }
    }

    private record Notification(UUID serviceOrderId, UUID customerId, ServiceOrderStatus status) {}
}
