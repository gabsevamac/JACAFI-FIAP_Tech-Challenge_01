package com.jacafi.tech.inventory.api.dto;

import java.util.List;

import com.jacafi.tech.inventory.application.InventoryPage;

/** One page of stock items as the API exposes it. */
public record InventoryPageResponse(
        List<InventoryItemResponse> content, int page, int size, long totalElements, int totalPages) {

    public static InventoryPageResponse from(InventoryPage page) {
        return new InventoryPageResponse(
                page.content().stream().map(InventoryItemResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
