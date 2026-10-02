package com.jacafi.tech.laboroperation.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

public interface LaborOperationRepository {
    LaborOperation save(LaborOperation operation);

    Optional<LaborOperation> findActiveById(UUID id);

    PageResult<LaborOperation> findActive(PageQuery query);

    boolean existsActiveWithName(String name);

    boolean existsActiveWithNameExcluding(String name, UUID id);
}
