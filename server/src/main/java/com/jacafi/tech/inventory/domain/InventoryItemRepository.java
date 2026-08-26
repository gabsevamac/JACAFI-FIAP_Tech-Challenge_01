package com.jacafi.tech.inventory.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the aggregate is stored and retrieved.
 *
 * <p>Scope is the aggregate's own lifecycle: persist one item with its open reservations, load one
 * item, answer the uniqueness question the registration rule depends on. Reads that exist to fill
 * a screen — listing, paging, filtering — are a delivery concern and live behind a read port in
 * the application layer.
 *
 * <p>Every lookup is restricted to active items. A removed item keeps its row so that withdrawals
 * already recorded still point at something, but it answers no query.
 */
public interface InventoryItemRepository {

    /** Inserts or updates the aggregate, open reservations included. */
    void save(InventoryItem item);

    Optional<InventoryItem> findActiveById(UUID id);

    /**
     * Loads the item intending to change it, holding it against concurrent writers until the
     * transaction ends.
     *
     * <p>Deliberately distinct from {@link #findActiveById}, and the reason the distinction is in
     * the port rather than hidden in the adapter: reserving is read-then-write. Two mechanics
     * reserving the last unit at the same time would both read one available, both find it
     * sufficient, and both write — leaving the item owing a unit it does not have. Reading under a
     * lock is what makes the check and the write one decision.
     *
     * <p>Pessimistic rather than a version column, because contention here is real (one popular
     * part, several open orders) and short-lived, and because losing the race should mean waiting
     * a few milliseconds rather than handing the caller a conflict to retry. It also keeps the
     * aggregate free of a version field it has no business meaning for.
     */
    Optional<InventoryItem> findActiveByIdForUpdate(UUID id);

    /**
     * Whether an active item already carries this name. Separate from a lookup because the
     * registration rule only needs the answer.
     */
    boolean existsActiveWithName(String name);

    /**
     * Same question, ignoring one item: what a rename needs, since an item keeping its own name is
     * not a duplicate of itself.
     */
    boolean existsActiveWithNameExcluding(String name, UUID excludedId);
}
