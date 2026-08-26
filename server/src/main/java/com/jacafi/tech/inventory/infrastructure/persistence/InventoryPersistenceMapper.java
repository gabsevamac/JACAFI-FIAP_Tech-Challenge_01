package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.Quantity;
import com.jacafi.tech.inventory.domain.Reservation;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Moves state between the aggregate and its storage shape. */
@Component
public class InventoryPersistenceMapper {

    public InventoryItemJpaEntity toEntity(InventoryItem item) {
        return new InventoryItemJpaEntity(item.getId(),
                item.getName(),
                item.getType(),
                item.getUnitPrice(),
                item.getStockOnHand().value(),
                item.getRegisteredAt(),
                item.getUpdatedAt(),
                item.getRemovedAt().orElse(null));
    }

    public InventoryReservationJpaEntity toEntity(UUID inventoryItemId, Reservation reservation) {
        return new InventoryReservationJpaEntity(reservation.id(),
                inventoryItemId,
                reservation.serviceOrderId(),
                reservation.quantity().value(),
                reservation.reservedAt());
    }

    public InventoryItem toDomain(InventoryItemJpaEntity entity,
                                  Collection<InventoryReservationJpaEntity> reservations) {
        List<Reservation> restored = reservations.stream()
                .map(InventoryPersistenceMapper::toDomain)
                .toList();

        return InventoryItem.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .unitPrice(entity.getUnitPrice())
                .stockOnHand(Quantity.of(entity.getStockOnHand()))
                .reservations(restored)
                .registeredAt(entity.getRegisteredAt())
                .updatedAt(entity.getUpdatedAt())
                .removedAt(entity.getRemovedAt())
                .restore();
    }

    private static Reservation toDomain(InventoryReservationJpaEntity entity) {
        return new Reservation(entity.getId(),
                entity.getServiceOrderId(),
                Quantity.of(entity.getQuantity()),
                entity.getReservedAt());
    }
}
