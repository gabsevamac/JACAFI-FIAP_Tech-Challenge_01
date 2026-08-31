package com.jacafi.tech.servicecatalog.application.service;

import java.util.UUID;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.servicecatalog.domain.exception.ServiceCatalogItemNotFoundException;

public class FindServiceCatalogItemService {
    private final ServiceCatalogRepositoryPort items;
    private final ServiceCatalogAccessPolicy access;

    public FindServiceCatalogItemService(ServiceCatalogRepositoryPort items, ServiceCatalogAccessPolicy access) {
        this.items = items;
        this.access = access;
    }

    public ServiceCatalogItem findById(UUID id) {
        access.requireOperationalAccess();
        return items.findActiveById(id).orElseThrow(ServiceCatalogItemNotFoundException::new);
    }
}
