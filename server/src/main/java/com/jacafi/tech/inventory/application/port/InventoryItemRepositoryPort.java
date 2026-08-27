package com.jacafi.tech.inventory.application.port;

import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.inventory.domain.entity.InventoryItem;

/** Write-side port. Locking is explicit for every decision based on available stock. */
public interface InventoryItemRepositoryPort {
    InventoryItem save(InventoryItem item, String actor);

    Optional<InventoryItem> findActiveById(UUID itemId);

    Optional<InventoryItem> findActiveByIdForUpdate(UUID itemId);

    boolean existsActiveWithName(String name);

    boolean existsActiveWithNameExcluding(String name, UUID itemId);
}
