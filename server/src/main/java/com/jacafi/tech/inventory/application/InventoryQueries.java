package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.MaterialType;

/**
 * Read port for queries that serve a screen rather than a business rule.
 *
 * <p>Separate from {@code InventoryItemRepository}, which loads one aggregate at a time to change
 * it. Listing the catalogue changes nothing and enforces nothing, so it does not belong behind the
 * same port — and keeping it here is what stops page and size from appearing in the domain.
 *
 * <p>Restricted to active items, like every other lookup in this slice.
 */
public interface InventoryQueries {

    /**
     * @param type null to list parts and supplies together
     * @param page zero-based page number
     * @param size maximum number of items per page
     */
    InventoryPage findActive(MaterialType type, int page, int size);
}
