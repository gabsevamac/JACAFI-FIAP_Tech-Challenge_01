package com.jacafi.tech.servicecatalog.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

@Component
public class ServiceCatalogPersistenceAdapter implements ServiceCatalogRepositoryPort {
    private final ServiceCatalogItemJpaRepository items;

    public ServiceCatalogPersistenceAdapter(ServiceCatalogItemJpaRepository items) {
        this.items = items;
    }

    @Override
    public ServiceCatalogItem save(ServiceCatalogItem item) {
        ServiceCatalogItemJpaEntity candidate = ServiceCatalogPersistenceMapper.toJpa(item);
        ServiceCatalogItemJpaEntity entity = items.findById(item.id())
                .map(existing -> {
                    if (existing.getVersion() != item.version()) {
                        throw new OptimisticLockingFailureException("Service catalog item changed concurrently");
                    }
                    existing.apply(item);
                    return existing;
                })
                .orElse(candidate);
        return ServiceCatalogPersistenceMapper.toDomain(items.save(entity));
    }

    @Override
    public Optional<ServiceCatalogItem> findActiveById(UUID id) {
        return items.findByIdAndActiveTrueAndDeletedAtIsNull(id).map(ServiceCatalogPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<ServiceCatalogItem> findActive(PageQuery query) {
        Pageable pageable = SpringDataPaging.toPageable(query);
        Page<ServiceCatalogItemJpaEntity> page = items.findByActiveTrueAndDeletedAtIsNull(pageable);
        return SpringDataPaging.toPageResult(page, query, ServiceCatalogPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsActiveWithName(String name) {
        return items.existsByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(name);
    }

    @Override
    public boolean existsActiveWithNameExcluding(String name, UUID id) {
        return items.existsByNameIgnoreCaseAndIdNotAndActiveTrueAndDeletedAtIsNull(name, id);
    }
}
