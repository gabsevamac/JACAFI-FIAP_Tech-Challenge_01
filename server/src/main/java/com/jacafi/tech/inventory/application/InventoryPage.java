package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.InventoryItem;

import java.util.List;
import java.util.Objects;

/**
 * One page of stock items, plus what a caller needs to ask for the next one.
 *
 * <p>Lives in the application layer, not the domain: paging is what a screen needs, not what the
 * aggregate guarantees. Declared here rather than reusing Spring Data's {@code Page} so the layer
 * stays free of the persistence framework and the read port below can be implemented by anything.
 *
 * @param content       the items on this page, never null
 * @param page          zero-based page number
 * @param size          maximum number of items per page
 * @param totalElements how many items match the query in total
 */
public record InventoryPage(List<InventoryItem> content, int page, int size, long totalElements) {

    public InventoryPage {
        Objects.requireNonNull(content, "content must not be null");
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    public int totalPages() {
        return (int) Math.ceilDiv(totalElements, size);
    }
}
