package com.jacafi.tech.servicecatalog.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.application.service.DeactivateServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.FindServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.ListServiceCatalogItemsService;
import com.jacafi.tech.servicecatalog.application.service.RegisterServiceCatalogItemService;
import com.jacafi.tech.servicecatalog.application.service.ServiceCatalogAccessPolicy;
import com.jacafi.tech.servicecatalog.application.service.UpdateServiceCatalogItemService;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;

@Configuration
public class ServiceCatalogConfiguration {
    @Bean
    ServiceCatalogAccessPolicy serviceCatalogAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new ServiceCatalogAccessPolicy(currentUser);
    }

    @Bean
    RegisterServiceCatalogItemService registerServiceCatalogItemService(
            ServiceCatalogRepositoryPort items,
            AuditTrailPort auditTrail,
            ServiceCatalogAccessPolicy access,
            Clock clock) {
        return new RegisterServiceCatalogItemService(items, auditTrail, access, clock);
    }

    @Bean
    FindServiceCatalogItemService findServiceCatalogItemService(
            ServiceCatalogRepositoryPort items, ServiceCatalogAccessPolicy access) {
        return new FindServiceCatalogItemService(items, access);
    }

    @Bean
    ListServiceCatalogItemsService listServiceCatalogItemsService(
            ServiceCatalogRepositoryPort items, ServiceCatalogAccessPolicy access) {
        return new ListServiceCatalogItemsService(items, access);
    }

    @Bean
    UpdateServiceCatalogItemService updateServiceCatalogItemService(
            ServiceCatalogRepositoryPort items,
            AuditTrailPort auditTrail,
            ServiceCatalogAccessPolicy access,
            Clock clock) {
        return new UpdateServiceCatalogItemService(items, auditTrail, access, clock);
    }

    @Bean
    DeactivateServiceCatalogItemService deactivateServiceCatalogItemService(
            ServiceCatalogRepositoryPort items,
            AuditTrailPort auditTrail,
            ServiceCatalogAccessPolicy access,
            Clock clock) {
        return new DeactivateServiceCatalogItemService(items, auditTrail, access, clock);
    }
}
