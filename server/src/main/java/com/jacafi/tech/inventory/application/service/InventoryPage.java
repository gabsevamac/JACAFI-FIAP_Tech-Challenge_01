package com.jacafi.tech.inventory.application.service;

import java.util.List;

import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.shared.application.PageResult;

public record InventoryPage(List<InventoryItem> content, int page, int size, long totalElements) {
    public InventoryPage {
        content = List.copyOf(content);
    }

    public PageResult<InventoryItem> toPageResult() {
        return PageResult.of(content, page, size, totalElements);
    }
}
