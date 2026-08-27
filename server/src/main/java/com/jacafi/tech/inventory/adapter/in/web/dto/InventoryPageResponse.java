package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.util.List;

import com.jacafi.tech.shared.application.PageResult;

public record InventoryPageResponse(
        List<InventoryItemResponse> content, int page, int size, long totalElements, int totalPages) {
    public static InventoryPageResponse from(PageResult<InventoryItemResponse> page) {
        return new InventoryPageResponse(
                page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
