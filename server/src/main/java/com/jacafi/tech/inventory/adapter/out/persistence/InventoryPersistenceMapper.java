package com.jacafi.tech.inventory.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.Reservation;
import com.jacafi.tech.inventory.domain.entity.Stock;

final class InventoryPersistenceMapper {
    private InventoryPersistenceMapper() {}

    static InventoryItemJpaEntity toJpa(InventoryItem item) {
        return new InventoryItemJpaEntity(
                item.id(),
                item.name(),
                item.type(),
                item.unitPrice(),
                item.stockOnHand().value());
    }

    static InventoryReservationJpaEntity toJpa(InventoryItem item, Reservation reservation) {
        return new InventoryReservationJpaEntity(
                reservation.id(),
                item.id(),
                reservation.serviceOrderId(),
                reservation.quantity().value(),
                reservation.reservedAt());
    }

    static InventoryItem toDomain(
            InventoryItemJpaEntity entity, Collection<InventoryReservationJpaEntity> reservations) {
        List<Reservation> restored = reservations.stream()
                .map(row -> new Reservation(row.id(), row.serviceOrderId(), Stock.of(row.quantity()), row.reservedAt()))
                .toList();
        return InventoryItem.restore(
                entity.id(),
                entity.name(),
                entity.type(),
                entity.unitPrice(),
                Stock.of(entity.stockOnHand()),
                restored,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt().orElse(null));
    }
}
