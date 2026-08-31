package com.jacafi.tech.inventory.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryReservationJpaRepository extends JpaRepository<InventoryReservationJpaEntity, UUID> {
    List<InventoryReservationJpaEntity> findByInventoryItemIdAndDeletedAtIsNull(UUID inventoryItemId);

    List<InventoryReservationJpaEntity> findByInventoryItemIdInAndDeletedAtIsNull(Collection<UUID> inventoryItemIds);
}
