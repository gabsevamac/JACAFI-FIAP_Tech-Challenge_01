package com.jacafi.tech.inventory.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;

/**
 * Reads one stock item, with its balance and its open reservations.
 *
 * <p>Absence is an exception rather than an empty {@code Optional} returned to the caller: this
 * lookup exists to serve a request for one specific item, and "not there" is the only other
 * answer. Keeping the decision here means the api layer does not repeat it.
 *
 * <p>Paged listing lives in {@link ListInventoryUseCase}, since it answers a different question.
 */
@Service
public class FindInventoryItemUseCase {

    private final InventoryItemRepository repository;

    public FindInventoryItemUseCase(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public InventoryItem byId(UUID inventoryItemId) {
        return repository
                .findActiveById(inventoryItemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(inventoryItemId));
    }
}
