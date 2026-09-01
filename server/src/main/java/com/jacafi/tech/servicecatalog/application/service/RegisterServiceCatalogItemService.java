package com.jacafi.tech.servicecatalog.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.servicecatalog.domain.exception.DuplicateServiceCatalogItemException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;

public class RegisterServiceCatalogItemService {
    private final ServiceCatalogRepositoryPort items;
    private final AuditTrailPort auditTrail;
    private final ServiceCatalogAccessPolicy access;
    private final Clock clock;

    public RegisterServiceCatalogItemService(
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
    public ServiceCatalogItem register(String name, String description, BigDecimal basePrice) {
        access.requireEmployee();
        ServiceCatalogItem item = ServiceCatalogItem.register(UUID.randomUUID(), name, description, basePrice, clock);
        if (items.existsActiveWithName(item.name())) {
            throw new DuplicateServiceCatalogItemException();
        }
        ServiceCatalogItem saved = items.save(item);
        String actor = access.currentActor();
        auditTrail.record(new AuditEvent("ServiceCatalogItem", saved.id(), "REGISTERED", actor, clock.instant()));
        return saved;
    }
}
