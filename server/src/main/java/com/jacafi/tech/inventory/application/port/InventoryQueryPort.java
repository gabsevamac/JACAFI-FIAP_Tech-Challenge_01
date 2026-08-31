package com.jacafi.tech.inventory.application.port;

import com.jacafi.tech.inventory.application.service.InventoryPage;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.application.PageQuery;

public interface InventoryQueryPort {
    InventoryPage findActive(MaterialType type, PageQuery query);
}
