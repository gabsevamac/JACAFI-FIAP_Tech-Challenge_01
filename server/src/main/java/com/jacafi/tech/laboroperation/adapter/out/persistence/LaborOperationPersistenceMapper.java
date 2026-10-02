package com.jacafi.tech.laboroperation.adapter.out.persistence;

import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;

final class LaborOperationPersistenceMapper {
    private LaborOperationPersistenceMapper() {}

    static LaborOperationJpaEntity toJpa(LaborOperation operation) {
        return new LaborOperationJpaEntity(
                operation.id(), operation.name(), operation.description(), operation.basePrice(), operation.active());
    }

    static LaborOperation toDomain(LaborOperationJpaEntity entity) {
        return LaborOperation.restore(
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
