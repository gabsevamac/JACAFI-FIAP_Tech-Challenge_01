package com.jacafi.tech.servicecatalog.domain.exception;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

public final class ServiceCatalogItemNotFoundException extends BusinessException {
    public ServiceCatalogItemNotFoundException() {
        super(ErrorCode.SERVICE_CATALOG_ITEM_NOT_FOUND);
    }
}
