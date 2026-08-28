package com.jacafi.tech.servicecatalog.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class DuplicateServiceCatalogItemException extends BusinessException {
    public DuplicateServiceCatalogItemException() {
        super(ErrorCode.DUPLICATE_SERVICE_CATALOG_ITEM);
    }
}
