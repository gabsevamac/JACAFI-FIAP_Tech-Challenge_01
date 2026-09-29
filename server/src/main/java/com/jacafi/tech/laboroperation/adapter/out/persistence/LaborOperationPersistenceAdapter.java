package com.jacafi.tech.laboroperation.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

@Component
public class LaborOperationPersistenceAdapter implements LaborOperationRepository {
    private final LaborOperationJpaRepository operations;

    public LaborOperationPersistenceAdapter(LaborOperationJpaRepository operations) {
        this.operations = operations;
    }

    @Override
    public LaborOperation save(LaborOperation operation) {
        LaborOperationJpaEntity candidate = LaborOperationPersistenceMapper.toJpa(operation);
        LaborOperationJpaEntity entity = operations
                .findById(operation.id())
                .map(existing -> {
                    if (existing.getVersion() != operation.version()) {
                        throw new OptimisticLockingFailureException("Labor operation changed concurrently");
                    }
                    existing.apply(operation);
                    return existing;
                })
                .orElse(candidate);
        return LaborOperationPersistenceMapper.toDomain(operations.save(entity));
    }

    @Override
    public Optional<LaborOperation> findActiveById(UUID id) {
        return operations.findByIdAndActiveTrueAndDeletedAtIsNull(id).map(LaborOperationPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<LaborOperation> findActive(PageQuery query) {
        Pageable pageable = SpringDataPaging.toPageable(query);
        Page<LaborOperationJpaEntity> page = operations.findByActiveTrueAndDeletedAtIsNull(pageable);
        return SpringDataPaging.toPageResult(page, query, LaborOperationPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsActiveWithName(String name) {
        return operations.existsByNameIgnoreCaseAndActiveTrueAndDeletedAtIsNull(name);
    }

    @Override
    public boolean existsActiveWithNameExcluding(String name, UUID id) {
        return operations.existsByNameIgnoreCaseAndIdNotAndActiveTrueAndDeletedAtIsNull(name, id);
    }
}
