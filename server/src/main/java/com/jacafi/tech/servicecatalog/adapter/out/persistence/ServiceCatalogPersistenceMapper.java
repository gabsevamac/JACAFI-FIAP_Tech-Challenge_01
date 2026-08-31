package com.jacafi.tech.servicecatalog.adapter.out.persistence;

import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;

final class ServiceCatalogPersistenceMapper {
    private ServiceCatalogPersistenceMapper() {}

    static ServiceCatalogItemJpaEntity toJpa(ServiceCatalogItem item) {
        return new ServiceCatalogItemJpaEntity(
                item.id(), item.name(), item.description(), item.basePrice(), item.active());
    }

    static ServiceCatalogItem toDomain(ServiceCatalogItemJpaEntity entity) {
        return ServiceCatalogItem.restore(
                entity.id(),
                entity.name(),
                entity.description(),
                entity.basePrice(),
                entity.active(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
