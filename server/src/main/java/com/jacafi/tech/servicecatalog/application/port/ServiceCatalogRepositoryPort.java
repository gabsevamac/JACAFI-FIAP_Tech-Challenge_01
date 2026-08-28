package com.jacafi.tech.servicecatalog.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public interface ServiceCatalogRepositoryPort {
    ServiceCatalogItem save(ServiceCatalogItem item);

    Optional<ServiceCatalogItem> findActiveById(UUID id);

    PageResult<ServiceCatalogItem> findActive(PageQuery query);

    boolean existsActiveWithName(String name);

    boolean existsActiveWithNameExcluding(String name, UUID id);
}
