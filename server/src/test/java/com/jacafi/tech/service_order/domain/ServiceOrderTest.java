package com.jacafi.tech.service_order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServiceOrderTest {

    private static final Instant T0 = Instant.parse("2026-08-26T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-08-26T10:30:00Z");
    private static final Instant T2 = Instant.parse("2026-08-26T11:00:00Z");
    private static final Instant T3 = Instant.parse("2026-08-26T11:30:00Z");
    private static final Instant T4 = Instant.parse("2026-08-26T12:00:00Z");
    private static final Instant T5 = Instant.parse("2026-08-26T14:00:00Z");
    private static final Instant T6 = Instant.parse("2026-08-26T15:00:00Z");

    private static final Clock CLOCK_0 = Clock.fixed(T0, ZoneOffset.UTC);
    private static final Clock CLOCK_1 = Clock.fixed(T1, ZoneOffset.UTC);
    private static final Clock CLOCK_2 = Clock.fixed(T2, ZoneOffset.UTC);
    private static final Clock CLOCK_3 = Clock.fixed(T3, ZoneOffset.UTC);
    private static final Clock CLOCK_4 = Clock.fixed(T4, ZoneOffset.UTC);
    private static final Clock CLOCK_5 = Clock.fixed(T5, ZoneOffset.UTC);
    private static final Clock CLOCK_6 = Clock.fixed(T6, ZoneOffset.UTC);

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VEHICLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Service OIL_CHANGE = Service.create(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Troca de óleo", new BigDecimal("150.00"));
    private static final Service ALIGNMENT = Service.create(
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Alinhamento", new BigDecimal("80.00"));

    private ServiceOrder openOrder() {
        return ServiceOrder.open(ORDER_ID, CUSTOMER_ID, VEHICLE_ID, CLOCK_0);
    }

    @Nested
    @DisplayName("opening service order")
    class Opening {

        @Test
        @DisplayName("opens order in RECEIVED status with initial zero total and records status history")
        void opensSuccessfully() {
            var order = openOrder();

            assertThat(order.getId()).isEqualTo(ORDER_ID);
            assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(order.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("0.00"));
            assertThat(order.getLaunchedServices()).isEmpty();
            assertThat(order.getDiagnosis()).isEmpty();
            assertThat(order.getStatusHistory()).hasSize(1);
            assertThat(order.getStatusHistory().get(0).toStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
            assertThat(order.getStatusHistory().get(0).occurredAt()).isEqualTo(T0);
        }

        @Test
        @DisplayName("opens with generated random ID")
        void opensWithGeneratedId() {
            var order = ServiceOrder.open(CUSTOMER_ID, VEHICLE_ID, CLOCK_0);

            assertThat(order.getId()).isNotNull();
            assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(order.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        }

        @Test
        @DisplayName("rejects null mandatory parameters when opening")
        void rejectsNullParameters() {
            assertThatNullPointerException()
                    .isThrownBy(() -> ServiceOrder.open(null, CUSTOMER_ID, VEHICLE_ID, CLOCK_0));

            assertThatNullPointerException().isThrownBy(() -> ServiceOrder.open(ORDER_ID, null, VEHICLE_ID, CLOCK_0));

            assertThatNullPointerException().isThrownBy(() -> ServiceOrder.open(ORDER_ID, CUSTOMER_ID, null, CLOCK_0));

            assertThatNullPointerException()
                    .isThrownBy(() -> ServiceOrder.open(ORDER_ID, CUSTOMER_ID, VEHICLE_ID, null));
        }
    }

    @Nested
    @DisplayName("diagnosis and launching services")
    class DiagnosisAndServices {

        @Test
        @DisplayName("transitions RECEIVED -> UNDER_DIAGNOSIS and records findings")
        void startsAndRecordsDiagnosis() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.UNDER_DIAGNOSIS);

            order.recordDiagnosis("Correia dentada gasta e vazamento de óleo", CLOCK_1);
            assertThat(order.getDiagnosis()).contains("Correia dentada gasta e vazamento de óleo");
        }

        @Test
        @DisplayName("launches service and updates order total")
        void launchesService() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            assertThat(order.getLaunchedServices()).hasSize(1);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("150.00"));

            order.launchService(ALIGNMENT, 2, CLOCK_2);
            assertThat(order.getLaunchedServices()).hasSize(2);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("310.00"));
        }

        @Test
        @DisplayName("re-launching same service increments quantity")
        void relaunchIncrementsQuantity() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.launchService(OIL_CHANGE, 2, CLOCK_2);

            assertThat(order.getLaunchedServices()).hasSize(1);
            assertThat(order.getLaunchedServices().get(0).getQuantity()).isEqualTo(3);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("450.00"));
        }

        @Test
        @DisplayName("removes launched service and recalculates total")
        void removesService() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.launchService(ALIGNMENT, 1, CLOCK_2);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("230.00"));

            boolean removed = order.removeLaunchedService(ALIGNMENT.getId());
            assertThat(removed).isTrue();
            assertThat(order.getLaunchedServices()).hasSize(1);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("rejects blank diagnosis")
        void rejectsBlankDiagnosis() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            assertThatIllegalArgumentException().isThrownBy(() -> order.recordDiagnosis("   ", CLOCK_1));
        }

        @Test
        @DisplayName("rejects startDiagnosis if not in RECEIVED")
        void rejectsStartDiagnosisWhenNotReceived() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            assertThatIllegalStateException()
                    .isThrownBy(() -> order.startDiagnosis(CLOCK_2))
                    .withMessageContaining("Cannot start diagnosis");
        }
    }

    @Nested
    @DisplayName("estimate and approval gate (the central invariant)")
    class EstimateAndApproval {

        @Test
        @DisplayName(
                "complete happy path: RECEIVED -> UNDER_DIAGNOSIS -> AWAITING_APPROVAL -> IN_PROGRESS -> COMPLETED -> DELIVERED")
        void completeHappyPath() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.recordDiagnosis("Troca de óleo necessária", CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);

            // Send estimate
            order.calculateAndSendEstimate(CLOCK_3);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);

            // Customer approves
            order.approveEstimate(CLOCK_4);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);

            // Complete services
            order.completeServices(CLOCK_5);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);

            // Deliver vehicle
            order.deliverVehicle(CLOCK_6);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
            assertThat(order.getStatus().isTerminal()).isTrue();

            assertThat(order.getStatusHistory()).hasSize(6);
        }

        @Test
        @DisplayName("rejects sending estimate without any launched services")
        void rejectsEstimateWithoutServices() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            assertThatIllegalStateException()
                    .isThrownBy(() -> order.calculateAndSendEstimate(CLOCK_2))
                    .withMessageContaining("without at least one launched service");
        }

        @Test
        @DisplayName("rejects execution without customer approval (invariable: no execution without approval)")
        void rejectsExecutionWithoutApproval() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.calculateAndSendEstimate(CLOCK_3);

            // Cannot complete services without being IN_PROGRESS
            assertThatIllegalStateException()
                    .isThrownBy(() -> order.completeServices(CLOCK_4))
                    .withMessageContaining("complete services");
        }

        @Test
        @DisplayName("customer rejects estimate -> REJECTED (terminal state)")
        void rejectsEstimate() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.calculateAndSendEstimate(CLOCK_3);

            order.rejectEstimate(CLOCK_4);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.REJECTED);
            assertThat(order.getStatus().isTerminal()).isTrue();

            // Terminal states cannot perform further operations
            assertThatIllegalStateException().isThrownBy(() -> order.approveEstimate(CLOCK_5));
            assertThatIllegalStateException().isThrownBy(() -> order.launchService(ALIGNMENT, 1, CLOCK_5));
        }

        @Test
        @DisplayName("estimate expires -> REJECTED")
        void expiresEstimate() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.calculateAndSendEstimate(CLOCK_3);

            order.expireEstimate(CLOCK_4);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("additional repairs flow")
    class AdditionalRepairs {

        @Test
        @DisplayName("additional repair loops back: IN_PROGRESS -> AWAITING_APPROVAL -> IN_PROGRESS")
        void additionalRepairLoop() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);
            order.calculateAndSendEstimate(CLOCK_3);
            order.approveEstimate(CLOCK_4);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);

            // During execution, mechanic finds alignment issue -> add additional repair
            order.addAdditionalRepair(CLOCK_5);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);

            // Launch additional service
            order.launchService(ALIGNMENT, 1, CLOCK_5);
            assertThat(order.getTotal()).isEqualTo(new BigDecimal("230.00"));

            // Customer approves additional repair estimate
            order.approveEstimate(CLOCK_6);
            assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("rejects additional repair if not currently in IN_PROGRESS")
        void rejectsAdditionalRepairWhenNotInProgress() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);

            assertThatIllegalStateException()
                    .isThrownBy(() -> order.addAdditionalRepair(CLOCK_2))
                    .withMessageContaining("add additional repair");
        }
    }

    @Nested
    @DisplayName("equality and immutability")
    class EqualityAndImmutability {

        @Test
        @DisplayName("service orders with same id are equal")
        void equalsAndHashCode() {
            var order1 = ServiceOrder.open(ORDER_ID, CUSTOMER_ID, VEHICLE_ID, CLOCK_0);
            var order2 = ServiceOrder.open(ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), CLOCK_0);
            var order3 = ServiceOrder.open(UUID.randomUUID(), CUSTOMER_ID, VEHICLE_ID, CLOCK_0);

            assertThat(order1).isEqualTo(order2);
            assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
            assertThat(order1).isNotEqualTo(order3);
            assertThat(order1).isNotEqualTo(null);
            assertThat(order1).isNotEqualTo("other");
        }

        @Test
        @DisplayName("launched services list is unmodifiable")
        void collectionsAreUnmodifiable() {
            var order = openOrder();
            order.startDiagnosis(CLOCK_1);
            order.launchService(OIL_CHANGE, 1, CLOCK_2);

            var list = order.getLaunchedServices();
            var dummyLaunched = LaunchedService.of(ALIGNMENT, 1);

            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class, () -> list.add(dummyLaunched));
        }
    }
}
