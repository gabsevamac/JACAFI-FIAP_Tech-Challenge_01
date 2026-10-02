package com.jacafi.tech.serviceorder.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.inventory.application.port.InventoryItemRepository;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.serviceorder.adapter.out.notification.OutboxStatusNotificationAdapter;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepository;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.application.service.ApproveEstimateService;
import com.jacafi.tech.serviceorder.application.service.CompleteServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.DeliverServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.FindServiceOrderStatusService;
import com.jacafi.tech.serviceorder.application.service.GenerateServiceOrderEstimateService;
import com.jacafi.tech.serviceorder.application.service.ListOperationalServiceOrdersService;
import com.jacafi.tech.serviceorder.application.service.OpenServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.RejectEstimateService;
import com.jacafi.tech.serviceorder.application.service.ServiceOrderAccessPolicy;
import com.jacafi.tech.serviceorder.application.service.StartServiceOrderDiagnosisService;
import com.jacafi.tech.serviceorder.application.service.UpdateServiceOrderStatusService;
import com.jacafi.tech.shared.adapter.out.persistence.EventOutboxPublisher;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepository;

@Configuration
public class ServiceOrderConfiguration {
    @Bean
    ServiceOrderAccessPolicy serviceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new ServiceOrderAccessPolicy(currentUser);
    }

    @Bean
    OpenServiceOrderService openServiceOrderService(
            ServiceOrderRepository orders,
            VehicleRepository vehicles,
            LaborOperationRepository laborOperations,
            InventoryItemRepository inventory,
            ReserveInventoryStockService reserveInventory,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new OpenServiceOrderService(
                orders,
                vehicles,
                laborOperations,
                inventory,
                reserveInventory,
                notifications,
                auditTrail,
                access,
                clock);
    }

    @Bean
    FindServiceOrderStatusService findServiceOrderStatusService(
            ServiceOrderRepository orders, ServiceOrderAccessPolicy access) {
        return new FindServiceOrderStatusService(orders, access);
    }

    @Bean
    ListOperationalServiceOrdersService listOperationalServiceOrdersService(
            ServiceOrderRepository orders, ServiceOrderAccessPolicy access) {
        return new ListOperationalServiceOrdersService(orders, access);
    }

    @Bean
    StartServiceOrderDiagnosisService startServiceOrderDiagnosisService(
            ServiceOrderRepository orders, AuditTrailPort auditTrail, ServiceOrderAccessPolicy access, Clock clock) {
        return new StartServiceOrderDiagnosisService(orders, auditTrail, access, clock);
    }

    @Bean
    GenerateServiceOrderEstimateService generateServiceOrderEstimateService(
            ServiceOrderRepository orders, AuditTrailPort auditTrail, ServiceOrderAccessPolicy access, Clock clock) {
        return new GenerateServiceOrderEstimateService(orders, auditTrail, access, clock);
    }

    @Bean
    ApproveEstimateService approveEstimateService(
            ServiceOrderRepository orders,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new ApproveEstimateService(orders, notifications, auditTrail, access, clock);
    }

    @Bean
    RejectEstimateService rejectEstimateService(
            ServiceOrderRepository orders,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new RejectEstimateService(orders, notifications, auditTrail, access, clock);
    }

    @Bean
    CompleteServiceOrderService completeServiceOrderService(
            ServiceOrderRepository orders, AuditTrailPort auditTrail, ServiceOrderAccessPolicy access, Clock clock) {
        return new CompleteServiceOrderService(orders, auditTrail, access, clock);
    }

    @Bean
    DeliverServiceOrderService deliverServiceOrderService(
            ServiceOrderRepository orders, AuditTrailPort auditTrail, ServiceOrderAccessPolicy access, Clock clock) {
        return new DeliverServiceOrderService(orders, auditTrail, access, clock);
    }

    @Bean
    StatusNotificationPort statusNotificationPort(EventOutboxPublisher publisher) {
        return new OutboxStatusNotificationAdapter(publisher);
    }

    @Bean
    UpdateServiceOrderStatusService updateServiceOrderStatusService(
            ServiceOrderRepository orders,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new UpdateServiceOrderStatusService(orders, notifications, auditTrail, access, clock);
    }
}
