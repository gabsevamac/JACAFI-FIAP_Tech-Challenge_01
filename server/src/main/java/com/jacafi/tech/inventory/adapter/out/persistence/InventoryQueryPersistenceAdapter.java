package com.jacafi.tech.inventory.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.jacafi.tech.inventory.application.port.InventoryQueryPort;
import com.jacafi.tech.inventory.application.service.InventoryPage;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;

@Component
public class InventoryQueryPersistenceAdapter implements InventoryQueryPort {
    private final InventoryItemJpaRepository items;
    private final InventoryReservationJpaRepository reservations;

    public InventoryQueryPersistenceAdapter(
            InventoryItemJpaRepository items, InventoryReservationJpaRepository reservations) {
        this.items = items;
        this.reservations = reservations;
    }

    @Override
    public InventoryPage findActive(MaterialType type, PageQuery query) {
        Page<InventoryItemJpaEntity> page = type == null
                ? items.findByDeletedAtIsNull(SpringDataPaging.toPageable(query))
                : items.findByTypeAndDeletedAtIsNull(type, SpringDataPaging.toPageable(query));
        return new InventoryPage(toDomain(page.getContent()), query.page(), query.size(), page.getTotalElements());
    }

    private List<InventoryItem> toDomain(List<InventoryItemJpaEntity> entities) {
        if (entities.isEmpty()) return List.of();
        Map<UUID, List<InventoryReservationJpaEntity>> byItem = new HashMap<>();
        for (InventoryReservationJpaEntity reservation : reservations.findByInventoryItemIdInAndDeletedAtIsNull(
                entities.stream().map(InventoryItemJpaEntity::id).toList()))
            byItem.computeIfAbsent(reservation.inventoryItemId(), ignored -> new ArrayList<>())
                    .add(reservation);
        return entities.stream()
                .map(entity -> InventoryPersistenceMapper.toDomain(entity, byItem.getOrDefault(entity.id(), List.of())))
                .toList();
    }
}
