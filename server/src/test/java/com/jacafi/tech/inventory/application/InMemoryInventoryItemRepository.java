package com.jacafi.tech.inventory.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.InventoryItemRepository;
import com.jacafi.tech.inventory.domain.MaterialType;

/**
 * Hand-written stand-in for the repository, implementing the same contract in a map.
 *
 * <p>Written rather than mocked on purpose. A mock would let a test assert which methods were
 * called; this asserts what the use case actually achieved — and, because it honours the "active
 * only" rule and the case-insensitive uniqueness rule of the port, the tests about duplicates and
 * about removed items mean something instead of replaying a stub.
 *
 * <p>{@link #saveCount()} exists because the map stores aggregates by reference: a use case that
 * mutated an aggregate and forgot to save it would otherwise still appear to work.
 */
class InMemoryInventoryItemRepository implements InventoryItemRepository, InventoryQueries {

    private final Map<UUID, InventoryItem> items = new LinkedHashMap<>();
    private int saveCount;

    @Override
    public void save(InventoryItem item) {
        items.put(item.getId(), item);
        saveCount++;
    }

    @Override
    public Optional<InventoryItem> findActiveById(UUID id) {
        return Optional.ofNullable(items.get(id)).filter(item -> !item.isRemoved());
    }

    /** No lock to take in a map; the contract above it is the same. */
    @Override
    public Optional<InventoryItem> findActiveByIdForUpdate(UUID id) {
        return findActiveById(id);
    }

    @Override
    public boolean existsActiveWithName(String name) {
        return activeItems().anyMatch(item -> matchesName(item, name));
    }

    @Override
    public boolean existsActiveWithNameExcluding(String name, UUID excludedId) {
        return activeItems().filter(item -> !item.getId().equals(excludedId)).anyMatch(item -> matchesName(item, name));
    }

    @Override
    public InventoryPage findActive(MaterialType type, int page, int size) {
        List<InventoryItem> matching = activeItems()
                .filter(item -> type == null || item.getType() == type)
                .sorted(Comparator.comparing(InventoryItem::getName))
                .toList();

        int from = Math.min(page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        return new InventoryPage(new ArrayList<>(matching.subList(from, to)), page, size, matching.size());
    }

    int saveCount() {
        return saveCount;
    }

    private static boolean matchesName(InventoryItem item, String name) {
        return item.getName().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT));
    }

    private Stream<InventoryItem> activeItems() {
        return items.values().stream().filter(item -> !item.isRemoved());
    }
}
