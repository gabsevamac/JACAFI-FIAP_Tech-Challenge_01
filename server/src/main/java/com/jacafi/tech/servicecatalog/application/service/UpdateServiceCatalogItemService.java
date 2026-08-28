package com.jacafi.tech.servicecatalog.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.servicecatalog.domain.exception.DuplicateServiceCatalogItemException;
import com.jacafi.tech.servicecatalog.domain.exception.ServiceCatalogItemNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class UpdateServiceCatalogItemService {
    private final ServiceCatalogRepositoryPort items;
    private final AuditTrailPort auditTrail;
    private final ServiceCatalogAccessPolicy access;
    private final Clock clock;

    public UpdateServiceCatalogItemService(
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
    public ServiceCatalogItem update(UUID id, String name, String description, BigDecimal basePrice) {
        access.requireManagementAccess();
        ServiceCatalogItem item = items.findActiveById(id).orElseThrow(ServiceCatalogItemNotFoundException::new);
        item.update(name, description, basePrice, clock);
        if (items.existsActiveWithNameExcluding(item.name(), item.id())) {
            throw new DuplicateServiceCatalogItemException();
        }
        ServiceCatalogItem saved = items.save(item);
        auditTrail.record(
                new AuditEvent("ServiceCatalogItem", saved.id(), "UPDATED", access.currentActor(), clock.instant()));
        return saved;
    }
}
