package com.jacafi.tech.inventory.application.service;

import java.util.UUID;

import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.exception.InventoryItemNotFoundException;

public class FindInventoryItemService {
    private final InventoryItemRepositoryPort items;
    private final InventoryAccessPolicy access;

    public FindInventoryItemService(InventoryItemRepositoryPort items, InventoryAccessPolicy access) {
        this.items = items;
        this.access = access;
    }

    public InventoryItem findById(UUID id) {
        access.requireOperationalAccess();
        return items.findActiveById(id).orElseThrow(InventoryItemNotFoundException::new);
    }
}
