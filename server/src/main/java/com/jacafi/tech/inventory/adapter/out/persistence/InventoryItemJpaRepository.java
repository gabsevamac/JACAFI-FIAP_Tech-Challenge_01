package com.jacafi.tech.inventory.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.jacafi.tech.inventory.domain.entity.MaterialType;

interface InventoryItemJpaRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {
    Optional<InventoryItemJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItemJpaEntity> findForUpdateByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, UUID id);

    Page<InventoryItemJpaEntity> findByDeletedAtIsNull(Pageable pageable);

    Page<InventoryItemJpaEntity> findByTypeAndDeletedAtIsNull(MaterialType type, Pageable pageable);
}
