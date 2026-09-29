package com.jacafi.tech.inventory.application.service;

import com.jacafi.tech.inventory.application.port.InventoryQueryPort;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.application.PageQuery;

public class ListInventoryItemsService {
    private final InventoryQueryPort queries;
    private final InventoryAccessPolicy access;

    public ListInventoryItemsService(InventoryQueryPort queries, InventoryAccessPolicy access) {
        this.queries = queries;
        this.access = access;
    }

    public InventoryPage list(MaterialType type, PageQuery query) {
        access.requireEmployee();
        return queries.findActive(type, query);
    }
}
