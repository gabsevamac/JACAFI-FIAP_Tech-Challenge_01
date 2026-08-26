package com.jacafi.tech.inventory.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Rows of the open reservations belonging to the aggregates in this package.
 *
 * <p>Not a repository in the domain sense: a reservation is never loaded on its own, only as part
 * of the item that owns it. This exists because the adapter reconciles those rows explicitly
 * rather than delegating to a cascade.
 */
interface InventoryReservationJpaRepository extends JpaRepository<InventoryReservationJpaEntity, UUID> {

    List<InventoryReservationJpaEntity> findByInventoryItemId(UUID inventoryItemId);

    /** One query for a whole page of items, instead of one query per item. */
    List<InventoryReservationJpaEntity> findByInventoryItemIdIn(Collection<UUID> inventoryItemIds);
}
