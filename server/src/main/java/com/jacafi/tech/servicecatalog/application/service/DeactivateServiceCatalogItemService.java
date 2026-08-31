package com.jacafi.tech.servicecatalog.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.exception.ServiceCatalogItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class DeactivateServiceCatalogItemService {
    private final ServiceCatalogRepositoryPort items;
    private final AuditTrailPort auditTrail;
    private final ServiceCatalogAccessPolicy access;
    private final Clock clock;

    public DeactivateServiceCatalogItemService(
            ServiceCatalogRepositoryPort items,
            AuditTrailPort auditTrail,
            ServiceCatalogAccessPolicy access,
            Clock clock) {
        this.items = items;
        this.auditTrail = auditTrail;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public void deactivate(UUID id) {
        access.requireManagementAccess();
        var item = items.findActiveById(id).orElseThrow(ServiceCatalogItemNotFoundException::new);
        item.deactivate(clock);
        items.save(item);
        auditTrail.record(
                new AuditEvent("ServiceCatalogItem", id, "DEACTIVATED", access.currentActor(), clock.instant()));
    }
}
