package com.jacafi.tech.servicecatalog.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;

public record ServiceCatalogItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {
    public static ServiceCatalogItemResponse from(ServiceCatalogItem item) {
        return new ServiceCatalogItemResponse(
                item.id(),
                item.name(),
                item.description(),
                item.basePrice(),
                item.active(),
                item.createdAt(),
                item.updatedAt(),
                item.version());
    }
}
