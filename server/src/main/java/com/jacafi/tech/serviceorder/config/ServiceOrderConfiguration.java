package com.jacafi.tech.serviceorder.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.serviceorder.adapter.out.notification.LoggingStatusNotificationAdapter;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.port.StatusNotificationPort;
import com.jacafi.tech.serviceorder.application.service.CompleteServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.DecideEstimateService;
import com.jacafi.tech.serviceorder.application.service.DeliverServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.FindServiceOrderStatusService;
import com.jacafi.tech.serviceorder.application.service.GenerateServiceOrderEstimateService;
import com.jacafi.tech.serviceorder.application.service.ListOperationalServiceOrdersService;
import com.jacafi.tech.serviceorder.application.service.OpenServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.ServiceOrderAccessPolicy;
import com.jacafi.tech.serviceorder.application.service.StartServiceOrderDiagnosisService;
import com.jacafi.tech.serviceorder.application.service.UpdateServiceOrderStatusService;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;

@Configuration
public class ServiceOrderConfiguration {
    @Bean
    ServiceOrderAccessPolicy serviceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new ServiceOrderAccessPolicy(currentUser);
    }

    @Bean
    OpenServiceOrderService openServiceOrderService(
            ServiceOrderRepositoryPort orders,
            VehicleRepositoryPort vehicles,
            ServiceCatalogRepositoryPort catalog,
            InventoryItemRepositoryPort inventory,
            ReserveInventoryStockService reserveInventory,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new OpenServiceOrderService(
                orders, vehicles, catalog, inventory, reserveInventory, auditTrail, access, clock);
    }

    @Bean
    FindServiceOrderStatusService findServiceOrderStatusService(
            ServiceOrderRepositoryPort orders, ServiceOrderAccessPolicy access) {
        return new FindServiceOrderStatusService(orders, access);
    }

    @Bean
    ListOperationalServiceOrdersService listOperationalServiceOrdersService(
            ServiceOrderRepositoryPort orders, ServiceOrderAccessPolicy access) {
        return new ListOperationalServiceOrdersService(orders, access);
    }

    @Bean
    StartServiceOrderDiagnosisService startServiceOrderDiagnosisService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new StartServiceOrderDiagnosisService(orders, auditTrail, access, clock);
    }

    @Bean
    GenerateServiceOrderEstimateService generateServiceOrderEstimateService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new GenerateServiceOrderEstimateService(orders, auditTrail, access, clock);
    }

    @Bean
    DecideEstimateService decideEstimateService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new DecideEstimateService(orders, auditTrail, access, clock);
    }

    @Bean
    CompleteServiceOrderService completeServiceOrderService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new CompleteServiceOrderService(orders, auditTrail, access, clock);
    }

    @Bean
    DeliverServiceOrderService deliverServiceOrderService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new DeliverServiceOrderService(orders, auditTrail, access, clock);
    }

    @Bean
    StatusNotificationPort statusNotificationPort() {
        return new LoggingStatusNotificationAdapter();
    }

    @Bean
    UpdateServiceOrderStatusService updateServiceOrderStatusService(
            ServiceOrderRepositoryPort orders,
            StatusNotificationPort notifications,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new UpdateServiceOrderStatusService(orders, notifications, auditTrail, access, clock);
    }
}
