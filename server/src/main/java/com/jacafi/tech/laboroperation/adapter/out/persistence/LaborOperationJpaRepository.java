package com.jacafi.tech.laboroperation.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface LaborOperationJpaRepository extends JpaRepository<LaborOperationJpaEntity, UUID> {
    Optional<LaborOperationJpaEntity> findByIdAndActiveTrueAndDeletedAtIsNull(UUID id);

    Page<LaborOperationJpaEntity> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

    boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrueAndDeletedAtIsNull(String name, UUID id);
}
