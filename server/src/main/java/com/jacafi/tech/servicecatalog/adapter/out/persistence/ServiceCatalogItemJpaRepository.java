package com.jacafi.tech.servicecatalog.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceCatalogItemJpaRepository extends JpaRepository<ServiceCatalogItemJpaEntity, UUID> {
    Optional<ServiceCatalogItemJpaEntity> findByIdAndActiveTrueAndDeletedAtIsNull(UUID id);

    Page<ServiceCatalogItemJpaEntity> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrueAndDeletedAtIsNull(String name, UUID id);
}
