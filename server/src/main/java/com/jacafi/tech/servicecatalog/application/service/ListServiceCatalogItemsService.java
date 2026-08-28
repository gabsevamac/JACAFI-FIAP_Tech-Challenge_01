package com.jacafi.tech.servicecatalog.application.service;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public class ListServiceCatalogItemsService {
    private final ServiceCatalogRepositoryPort items;
    private final ServiceCatalogAccessPolicy access;

    public ListServiceCatalogItemsService(ServiceCatalogRepositoryPort items, ServiceCatalogAccessPolicy access) {
        this.items = items;
        this.access = access;
    }

    public PageResult<ServiceCatalogItem> list(PageQuery query) {
        access.requireOperationalAccess();
        return items.findActive(query);
    }
}
